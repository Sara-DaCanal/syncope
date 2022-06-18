package org.apache.syncope.core.spring.security;

import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
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
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;


@RunWith(Parameterized.class)
public class TestAuthDataAccessorBis {
    private String username;
    private String dependencyKey;
    private AuthDataAccessor auth;
    private boolean expected;
    static MockedStatic<ApplicationContextProvider> util;

    private static SecurityProperties sp = new SecurityProperties();

    public TestAuthDataAccessorBis(values username, values dependency, boolean expected){
        configure(username, dependency, expected);
    }

    public UserDAO getMockedUserDAO(){
        UserDAO user = Mockito.mock(UserDAO.class);
        //Mockito.when(user.findKey("abcde")).thenReturn("");
        User u = new DummyUser();
        Mockito.when(user.findByUsername("username")).thenReturn( u);
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

    private void configure(values username, values dependency, boolean expected){
        switch (username){
            case NULL:
                this.username=null;
                break;
            case VOID:
                this.username="";
                break;
            case VALID:
                this.username="username";
                break;
            case INVALID:
                this.username="not_a_user";
                break;
            case ADMIN:
                this.username="admin";
                break;
            case ANONYM:
                this.username="anonymous";
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
                break;
            case INVALID:
                this.dependencyKey="not_exist";
                break;
        }
        this.expected=expected;
        this.auth = new AuthDataAccessor(sp, null, getMockedUserDAO(), getMockedGroupDAO(), null, null, null, null, null, getDelegationDAO(false), null, null, null, null );
    }


    @Parameterized.Parameters
    public static Collection parameters(){
        return Arrays.asList(new Object[][]{
                {values.ADMIN, values.NULL, false},
                {values.ANONYM, values.NULL, false},
                {values.NULL, values.VALID, false},
                {values.VOID, values.VALID, false},
                {values.VALID, values.VALID, false},
                {values.INVALID, values.VALID, false},

        });
    }

    @Test
    public void test(){
        try{
            Set<SyncopeGrantedAuthority> set = auth.getAuthorities(username, dependencyKey);
            Assert.assertFalse(expected);
            Assert.assertNotNull(set);
        }catch (UsernameNotFoundException e){
            Assert.assertTrue(expected);
        }
    }

    @AfterClass
    public static void tearDown(){
        util.close();
    }

    private enum values{
        VALID, VOID, NULL, INVALID, ADMIN, ANONYM
    }
}

