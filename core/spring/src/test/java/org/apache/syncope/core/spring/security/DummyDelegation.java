package org.apache.syncope.core.spring.security;

import org.apache.syncope.core.persistence.api.entity.*;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DummyDelegation implements Delegation {

    private boolean emptyRole;

    public DummyDelegation(boolean emptyRole){
        this.emptyRole = emptyRole;
    }

    private class DummyRole implements Role{

        @Override
        public String getKey() {
            return null;
        }

        @Override
        public void setKey(String key) {

        }

        @Override
        public Set<String> getEntitlements() {
            return Set.of("all");
        }

        @Override
        public boolean add(Realm realm) {
            return false;
        }

        @Override
        public List<? extends Realm> getRealms() {
            return new ArrayList<>();
        }

        @Override
        public boolean add(DynRealm dynRealm) {
            return false;
        }

        @Override
        public List<? extends DynRealm> getDynRealms() {
            return new ArrayList<>();
        }

        @Override
        public DynRoleMembership getDynMembership() {
            return null;
        }

        @Override
        public void setDynMembership(DynRoleMembership dynMembership) {

        }

        @Override
        public String getAnyLayout() {
            return null;
        }

        @Override
        public void setAnyLayout(String anyLayout) {

        }

        @Override
        public boolean add(Privilege privilege) {
            return false;
        }

        @Override
        public Set<? extends Privilege> getPrivileges(Application application) {
            return null;
        }

        @Override
        public Set<? extends Privilege> getPrivileges() {
            return null;
        }
    }
    @Override
    public User getDelegating() {
        return new DummyUser();
    }

    @Override
    public void setDelegating(User delegating) {

    }

    @Override
    public User getDelegated() {
        return null;
    }

    @Override
    public void setDelegated(User delegated) {

    }

    @Override
    public void setStart(OffsetDateTime start) {

    }

    @Override
    public OffsetDateTime getStart() {
        return null;
    }

    @Override
    public void setEnd(OffsetDateTime end) {

    }

    @Override
    public OffsetDateTime getEnd() {
        return null;
    }

    @Override
    public boolean add(Role role) {
        return false;
    }

    @Override
    public Set<? extends Role> getRoles() {
        if(emptyRole) return Set.of();
        else return Set.of(new DummyRole());
    }

    @Override
    public String getKey() {
        return null;
    }
}
