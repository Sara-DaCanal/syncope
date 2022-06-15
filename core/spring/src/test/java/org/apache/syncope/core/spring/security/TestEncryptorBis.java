package org.apache.syncope.core.spring.security;

import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import javax.crypto.*;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestEncryptorBis{
    private  static Encryptor encryptor;
    private static Encryptor encryptor1;
    private static Encryptor encryptor2;
    private static Encryptor encryptor3;
    private String value;
    private CipherAlgorithm cipherAlgorithm;
    private String encoded;
    private static MockedStatic<ApplicationContextProvider> util;


    public TestEncryptorBis(string value, CipherAlgorithm cipherAlgorithm){
        configure(value, cipherAlgorithm);
    }

    @BeforeClass
    public static void setUp(){
        DefaultListableBeanFactory factory=new DefaultListableBeanFactory();

        factory.registerSingleton("securityProperties", new SecurityProperties());

        util = Mockito.mockStatic(ApplicationContextProvider.class);
        util.when(ApplicationContextProvider::getBeanFactory).thenReturn(factory);
        util.when(ApplicationContextProvider::getApplicationContext).thenReturn(new DummyConfigurableApplicationContext(factory));
        encryptor = Encryptor.getInstance("");
        encryptor1=Encryptor.getInstance(null);
        encryptor2=Encryptor.getInstance("short");
        encryptor3=Encryptor.getInstance("long_secret_key_1234");
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
            case NULL:
                this.value=null;
        }
    }

    @Parameterized.Parameters
    public static Collection parameters(){
        return Arrays.asList(new Object[][]{
                {string.VALID, CipherAlgorithm.AES},
                {string.NULL, CipherAlgorithm.AES},
                {string.VOID, CipherAlgorithm.AES},
                {string.VALID, CipherAlgorithm.SHA},
                {string.NULL, CipherAlgorithm.SHA},
                {string.VOID, CipherAlgorithm.SHA},
                {string.VALID, CipherAlgorithm.SHA1},
                {string.NULL, CipherAlgorithm.SHA1},
                {string.VOID, CipherAlgorithm.SHA1},
                {string.VALID, CipherAlgorithm.SHA256},
                {string.NULL, CipherAlgorithm.SHA256},
                {string.VOID, CipherAlgorithm.SHA256},
                {string.VALID, CipherAlgorithm.SHA512},
                {string.NULL, CipherAlgorithm.SHA512},
                {string.VOID, CipherAlgorithm.SHA512},
                {string.VALID, CipherAlgorithm.SMD5},
                {string.NULL, CipherAlgorithm.SMD5},
                {string.VOID, CipherAlgorithm.SMD5},
                {string.VALID, CipherAlgorithm.SSHA},
                {string.VOID, CipherAlgorithm.SSHA},
                {string.NULL, CipherAlgorithm.SSHA},
                {string.VALID, CipherAlgorithm.SSHA1},
                {string.NULL, CipherAlgorithm.SSHA1},
                {string.VOID, CipherAlgorithm.SSHA1},
                {string.VALID, CipherAlgorithm.SSHA256},
                {string.NULL, CipherAlgorithm.SSHA256},
                {string.VOID, CipherAlgorithm.SSHA256},
                {string.VALID, CipherAlgorithm.SSHA512},
                {string.VOID, CipherAlgorithm.SSHA512},
                {string.NULL, CipherAlgorithm.SSHA512},
                {string.VALID, CipherAlgorithm.BCRYPT},
                {string.VALID, CipherAlgorithm.BCRYPT},
                {string.VALID, CipherAlgorithm.BCRYPT},
                {string.VALID, null},
                {string.NULL, null},
                {string.VOID, null},
        });
    }



    @Test
    public void test() {
        try {
            encoded = encryptor.encode(value, cipherAlgorithm);
            if(value==null) Assert.assertNull(encoded);
            else Assert.assertTrue(encryptor.verify(value, cipherAlgorithm, encoded));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException e) {
            Assert.fail();
        }
    }

    @Test
    public void test1() {
        try {
            encoded = encryptor1.encode(value, cipherAlgorithm);
            if(value==null) Assert.assertNull(encoded);
            else Assert.assertTrue(encryptor1.verify(value, cipherAlgorithm, encoded));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException e) {
            Assert.fail();
        }
    }

    @Test
    public void test2() {
        try {
            encoded = encryptor2.encode(value, cipherAlgorithm);
            if(value==null) Assert.assertNull(encoded);
            else Assert.assertTrue(encryptor2.verify(value, cipherAlgorithm, encoded));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException e) {
            Assert.fail();
        }
    }

    @Test
    public void test3() {
        try {
            encoded = encryptor3.encode(value, cipherAlgorithm);
            if(value==null) Assert.assertNull(encoded);
            else Assert.assertTrue(encryptor3.verify(value, cipherAlgorithm, encoded));
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
        VALID, VOID, NULL
    }

}