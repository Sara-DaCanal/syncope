package org.apache.syncope.core.spring.security;

import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;


import java.util.*;
import java.util.stream.Collectors;

import static org.apache.syncope.core.spring.security.AuthDataAccessor.ANONYMOUS_AUTHORITIES;

@RunWith(Parameterized.class)
public class TestAuthDataPIT {
    private String username;
    private String dependencyKey;
    private AuthDataAccessor auth;
    private Set<SyncopeGrantedAuthority> expected;
    static MockedStatic<ApplicationContextProvider> util;

    private static SecurityProperties sp = new SecurityProperties();

    public TestAuthDataPIT(values username, values dependency){
        configure(username, dependency);
    }

    public UserDAO getMockedUserDAO(){
        UserDAO user = Mockito.mock(UserDAO.class);
        User u = new DummyUser();
        Mockito.when(user.findByUsername("username")).thenReturn(u);
        return user;
    }

    public GroupDAO getMockedGroupDAO(){
        GroupDAO group = Mockito.mock(GroupDAO.class);
        Mockito.when(group.findOwnedByUser("username")).thenReturn(new ArrayList<>());
        return group;
    }

    public DelegationDAO getDelegationDAO(boolean emptyRole){
        DelegationDAO delegation = Mockito.mock(DelegationDAO.class);
        Mockito.when(delegation.find("delegate")).thenReturn(new DummyDelegation(emptyRole));
        return delegation;
    }

    @BeforeClass
    public static void setUp(){
        sp.setAnonymousUser("anonymous");
        sp.setAdminUser("admin");
        DefaultListableBeanFactory factory=new DefaultListableBeanFactory();

        factory.registerSingleton("securityProperties", sp);

        util = Mockito.mockStatic(ApplicationContextProvider.class);
        util.when(ApplicationContextProvider::getBeanFactory).thenReturn(factory);
        util.when(ApplicationContextProvider::getApplicationContext).thenReturn(new DummyConfigurableApplicationContext(factory));
    }

    private void configure(values username, values dependency){
        this.auth = new AuthDataAccessor(sp, null, getMockedUserDAO(), getMockedGroupDAO(), null, null, null, null, null, getDelegationDAO(false), null, null, null, null);
        switch (username){
            case NULL:
                this.username=null;
                break;
            case VOID:
                this.username="";
                break;
            case VALID:
                this.username="username";
                this.expected=Set.of();
                break;
            case INVALID:
                this.username="not_a_user";
                break;
            case ADMIN:
                this.username="admin";
                this.expected= EntitlementsHolder.getInstance().getValues().stream().
                        map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                        collect(Collectors.toSet());
                break;
            case ANONYM:
                this.username="anonymous";
                this.expected=ANONYMOUS_AUTHORITIES;
                break;
        }
        switch (dependency){
            case NULL:
                this.dependencyKey=null;
                break;
            case VOID:
                this.dependencyKey="";
                break;
            case VALID:
                this.dependencyKey="delegate";
                Map<String,Set<String>> delegMap = new HashMap<>();
                delegMap.put("all", new HashSet<>());
                this.expected = auth.buildAuthorities(delegMap);
                break;
            case INVALID:
                this.dependencyKey="not_exist";
                break;
        }
    }


    @Parameterized.Parameters
    public static Collection parameters(){
        return Arrays.asList(new Object[][]{

                {values.VALID, values.NULL},
                {values.ADMIN, values.NULL},
                {values.ANONYM, values.NULL},
                {values.NULL, values.VALID},
        });
    }

    @Test
    public void test(){
        Set<SyncopeGrantedAuthority> set = auth.getAuthorities(username, dependencyKey);
        Assert.assertEquals(expected, set);
    }

    @AfterClass
    public static void tearDown(){
        util.close();
    }

    private enum values{
        VALID, VOID, NULL, INVALID, ADMIN, ANONYM
    }


}
