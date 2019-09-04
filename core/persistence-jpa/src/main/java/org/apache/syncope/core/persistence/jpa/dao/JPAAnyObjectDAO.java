/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAARelationship;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.provisioning.api.event.AnyDeletedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class JPAAnyObjectDAO extends AbstractAnyDAO<AnyObject> implements AnyObjectDAO {

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Override
    protected AnyUtils init() {
        return anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT);
    }

    @Transactional(readOnly = true)
    @Override
    public String findKey(final String name) {
        return findKey(name, JPAAnyObject.TABLE);
    }

    @Transactional(readOnly = true)
    @Override
    public Date findLastChange(final String key) {
        return findLastChange(key, JPAAnyObject.TABLE);
    }

    @Override
    public Map<AnyType, Integer> countByType() {
        Query query = entityManager().createQuery(
                "SELECT e.type, COUNT(e) AS countByType FROM  " + anyUtils().anyClass().getSimpleName() + " e "
                + "GROUP BY e.type ORDER BY countByType DESC");
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<AnyType, Integer> countByRealm = new LinkedHashMap<>(results.size());
        for (Object[] result : results) {
            countByRealm.put((AnyType) result[0], ((Number) result[1]).intValue());
        }

        return Collections.unmodifiableMap(countByRealm);
    }

    @Override
    public Map<String, Integer> countByRealm(final AnyType anyType) {
        Query query = entityManager().createQuery(
                "SELECT e.realm, COUNT(e) FROM  " + anyUtils().anyClass().getSimpleName() + " e "
                + "WHERE e.type=:type GROUP BY e.realm");
        query.setParameter("type", anyType);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results.stream().collect(Collectors.toMap(
                result -> ((Realm) result[0]).getFullPath(),
                result -> ((Number) result[1]).intValue()));
    }

    @Override
    protected void securityChecks(final AnyObject anyObject) {
        Map<String, Set<String>> authorizations = AuthContextUtils.getAuthorizations();
        Set<String> authRealms = authorizations.containsKey(AnyEntitlement.READ.getFor(anyObject.getType().getKey()))
                ? authorizations.get(AnyEntitlement.READ.getFor(anyObject.getType().getKey()))
                : Collections.emptySet();
        boolean authorized = authRealms.stream().
                anyMatch(realm -> anyObject.getRealm().getFullPath().startsWith(realm));
        if (!authorized) {
            authorized = findDynRealms(anyObject.getKey()).stream().
                    filter(dynRealm -> authRealms.contains(dynRealm)).
                    count() > 0;
        }
        if (authRealms.isEmpty() || !authorized) {
            throw new DelegatedAdministrationException(
                    anyObject.getRealm().getFullPath(), AnyTypeKind.ANY_OBJECT.name(), anyObject.getKey());
        }
    }

    @Override
    public AnyObject findByName(final String name) {
        TypedQuery<AnyObject> query = entityManager().createQuery(
                "SELECT e FROM " + anyUtils().anyClass().getSimpleName() + " e WHERE e.name = :name", AnyObject.class);
        query.setParameter("name", name);

        AnyObject result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No any object found with name {}", name, e);
        }

        return result;
    }

    @Override
    public AMembership findMembership(final String key) {
        return entityManager().find(JPAAMembership.class, key);
    }

    @Override
    public List<Relationship<Any<?>, AnyObject>> findAllRelationships(final AnyObject anyObject) {
        List<Relationship<Any<?>, AnyObject>> result = new ArrayList<>();

        @SuppressWarnings("unchecked")
        TypedQuery<Relationship<Any<?>, AnyObject>> aquery =
                (TypedQuery<Relationship<Any<?>, AnyObject>>) entityManager().createQuery(
                        "SELECT e FROM " + JPAARelationship.class.getSimpleName()
                        + " e WHERE e.rightEnd=:anyObject OR e.leftEnd=:anyObject");
        aquery.setParameter("anyObject", anyObject);
        result.addAll(aquery.getResultList());

        @SuppressWarnings("unchecked")
        TypedQuery<Relationship<Any<?>, AnyObject>> uquery =
                (TypedQuery<Relationship<Any<?>, AnyObject>>) entityManager().createQuery(
                        "SELECT e FROM " + JPAURelationship.class.getSimpleName()
                        + " e WHERE e.rightEnd=:anyObject");
        uquery.setParameter("anyObject", anyObject);
        result.addAll(uquery.getResultList());

        return result;
    }

    @Override
    public int count() {
        Query query = entityManager().createQuery(
                "SELECT COUNT(e) FROM  " + anyUtils().anyClass().getSimpleName() + " e");
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public List<AnyObject> findAll(final int page, final int itemsPerPage) {
        TypedQuery<AnyObject> query = entityManager().createQuery(
                "SELECT e FROM  " + anyUtils().anyClass().getSimpleName() + " e ORDER BY e.id", AnyObject.class);
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));
        query.setMaxResults(itemsPerPage);

        return query.getResultList();
    }

    @Override
    public List<String> findAllKeys(final int page, final int itemsPerPage) {
        return findAllKeys(JPAAnyObject.TABLE, page, itemsPerPage);
    }

    protected Pair<AnyObject, Pair<Set<String>, Set<String>>> doSave(final AnyObject anyObject) {
        AnyObject merged = super.save(anyObject);
        publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, merged, AuthContextUtils.getDomain()));

        Pair<Set<String>, Set<String>> dynGroupMembs = groupDAO.refreshDynMemberships(merged);
        dynRealmDAO.refreshDynMemberships(merged);

        return Pair.of(merged, dynGroupMembs);
    }

    @Override
    public AnyObject save(final AnyObject anyObject) {
        return doSave(anyObject).getLeft();
    }

    @Override
    public Pair<Set<String>, Set<String>> saveAndGetDynGroupMembs(final AnyObject anyObject) {
        return doSave(anyObject).getRight();
    }

    protected List<ARelationship> findARelationships(final AnyObject anyObject) {
        TypedQuery<ARelationship> query = entityManager().createQuery(
                "SELECT e FROM " + JPAARelationship.class.getSimpleName()
                + " e WHERE e.rightEnd=:anyObject", ARelationship.class);
        query.setParameter("anyObject", anyObject);

        return query.getResultList();
    }

    protected List<URelationship> findURelationships(final AnyObject anyObject) {
        TypedQuery<URelationship> query = entityManager().createQuery(
                "SELECT e FROM " + JPAURelationship.class.getSimpleName()
                + " e WHERE e.rightEnd=:anyObject", URelationship.class);
        query.setParameter("anyObject", anyObject);

        return query.getResultList();
    }

    @Override
    public void delete(final AnyObject anyObject) {
        groupDAO.removeDynMemberships(anyObject);
        dynRealmDAO.removeDynMemberships(anyObject.getKey());

        findARelationships(anyObject).forEach(relationship -> {
            relationship.getLeftEnd().getRelationships().remove(relationship);
            save(relationship.getLeftEnd());

            entityManager().remove(relationship);
        });
        findURelationships(anyObject).forEach(relationship -> {
            relationship.getLeftEnd().getRelationships().remove(relationship);
            userDAO.save(relationship.getLeftEnd());

            entityManager().remove(relationship);
        });

        entityManager().remove(anyObject);
        publisher.publishEvent(new AnyDeletedEvent(
                this, AnyTypeKind.ANY_OBJECT, anyObject.getKey(), anyObject.getName(), AuthContextUtils.getDomain()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public List<Group> findDynGroups(final String key) {
        Query query = entityManager().createNativeQuery(
                "SELECT group_id FROM " + JPAGroupDAO.ADYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, key);

        List<Group> result = new ArrayList<>();
        query.getResultList().stream().map(resultKey -> resultKey instanceof Object[]
                ? (String) ((Object[]) resultKey)[0]
                : ((String) resultKey)).
                forEach(groupKey -> {
                    Group group = groupDAO.find(groupKey.toString());
                    if (group == null) {
                        LOG.error("Could not find group {}, even though returned by the native query", groupKey);
                    } else if (!result.contains(group)) {
                        result.add(group);
                    }
                });
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<Group> findAllGroups(final AnyObject anyObject) {
        Set<Group> result = new HashSet<>();
        result.addAll(anyObject.getMemberships().stream().
                map(membership -> membership.getRightEnd()).collect(Collectors.toSet()));
        result.addAll(findDynGroups(anyObject.getKey()));

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllGroupKeys(final AnyObject anyObject) {
        return findAllGroups(anyObject).stream().map(group -> group.getKey()).collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<ExternalResource> findAllResources(final AnyObject anyObject) {
        Set<ExternalResource> result = new HashSet<>();
        result.addAll(anyObject.getResources());
        findAllGroups(anyObject).forEach(group -> result.addAll(group.getResources()));

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<String> findAllResourceKeys(final String key) {
        return findAllResources(authFind(key)).stream().map(resource -> resource.getKey()).collect(Collectors.toList());
    }
}
