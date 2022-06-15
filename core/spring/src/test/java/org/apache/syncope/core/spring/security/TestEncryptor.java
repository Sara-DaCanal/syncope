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
public class TestEncryptor{
    private  static Encryptor encryptor;
    private String value;
    private CipherAlgorithm cipherAlgorithm;
    private String encoded;

    
    public TestEncryptor(string value, CipherAlgorithm cipherAlgorithm){
        configure(value, cipherAlgorithm);
    }

   @BeforeClass
    public static void setUp(){
        DefaultListableBeanFactory factory=new DefaultListableBeanFactory();

        factory.registerSingleton("securityProperties", new SecurityProperties());

           MockedStatic<ApplicationContextProvider> util = Mockito.mockStatic(ApplicationContextProvider.class);
            util.when(ApplicationContextProvider::getBeanFactory).thenReturn(factory);
            util.when(ApplicationContextProvider::getApplicationContext).thenReturn(new DummyConfigurableApplicationContext(factory));
        encryptor = Encryptor.getInstance();
    }

    private void configure(string value, CipherAlgorithm cipherAlgorithm) {
       // encryptor = Encryptor.getInstance();
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
                {string.VALID, null},
                {string.NULL, null},
                {string.VOID, null},
        });
    }



    @Test
    public void test() {
        try {

            encoded = encryptor.encode(value, cipherAlgorithm);
            if(value==null) Assert.assertFalse(encryptor.verify(value, cipherAlgorithm, encoded));
            else Assert.assertTrue(encryptor.verify(value, cipherAlgorithm, encoded));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException e) {
           Assert.fail();
        }
    }

    private enum string{
        VALID, VOID, NULL
    }

}
