package org.apache.syncope.core.spring.security;

import org.apache.syncope.common.lib.types.CipherAlgorithm;
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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestEncryptorPIT{
    private  static Encryptor encryptor;
    private String value;
    private CipherAlgorithm cipherAlgorithm;
    private String encoded;
    static MockedStatic<ApplicationContextProvider> util;


    public TestEncryptorPIT(string value, CipherAlgorithm cipherAlgorithm){
        configure(value, cipherAlgorithm);
    }

    @BeforeClass
    public static void setUp(){
        DefaultListableBeanFactory factory=new DefaultListableBeanFactory();

        factory.registerSingleton("securityProperties", new SecurityProperties());

        util = Mockito.mockStatic(ApplicationContextProvider.class);
        util.when(ApplicationContextProvider::getBeanFactory).thenReturn(factory);
        util.when(ApplicationContextProvider::getApplicationContext).thenReturn(new DummyConfigurableApplicationContext(factory));
        encryptor = Encryptor.getInstance();
    }

    private void configure(string value, CipherAlgorithm cipherAlgorithm) {
        this.cipherAlgorithm= cipherAlgorithm;
        switch (value){
            case VALID:
                this.value="abcde";
                break;
            case VOID:
                this.value="";
                break;
        }
    }

    @Parameterized.Parameters
    public static Collection parameters(){
        return Arrays.asList(new Object[][]{
                {string.VALID, CipherAlgorithm.AES},
                {string.VOID, CipherAlgorithm.AES},
                {string.VALID, CipherAlgorithm.SHA},
                {string.VOID, CipherAlgorithm.SHA},
                {string.VALID, CipherAlgorithm.SHA1},
                {string.VOID, CipherAlgorithm.SHA1},
                {string.VALID, CipherAlgorithm.SHA256},
                {string.VOID, CipherAlgorithm.SHA256},
                {string.VALID, CipherAlgorithm.SHA512},
                {string.VOID, CipherAlgorithm.SHA512},
                {string.VALID, CipherAlgorithm.SMD5},
                {string.VOID, CipherAlgorithm.SMD5},
                {string.VALID, CipherAlgorithm.SSHA},
                {string.VOID, CipherAlgorithm.SSHA},
                {string.VALID, CipherAlgorithm.SSHA1},
                {string.VOID, CipherAlgorithm.SSHA1},
                {string.VALID, CipherAlgorithm.SSHA256},
                {string.VOID, CipherAlgorithm.SSHA256},
                {string.VALID, CipherAlgorithm.SSHA512},
                {string.VOID, CipherAlgorithm.SSHA512},
                {string.VALID, CipherAlgorithm.BCRYPT},
                {string.VOID, CipherAlgorithm.BCRYPT},
                {string.VALID, null},
                {string.VOID, null}
        });
    }



    @Test
    public void test() {
        try {
            encoded = encryptor.encode(value, cipherAlgorithm);
            if(cipherAlgorithm==null) Assert.assertEquals(encoded, encryptor.encode(value,CipherAlgorithm.AES));
            else if(cipherAlgorithm.isSalted() || cipherAlgorithm.equals(CipherAlgorithm.BCRYPT)) Assert.assertNotEquals(encoded, encryptor.encode(value, cipherAlgorithm));
            else Assert.assertEquals(encoded, encryptor.encode(value,cipherAlgorithm));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException e) {
            Assert.fail();
        }
    }

    @AfterClass
    public static void tearDown(){
        util.close();
    }

    private enum string{
        VALID, VOID
    }

}

