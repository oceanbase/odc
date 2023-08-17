/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.encryption;

import com.oceanbase.odc.common.crypto.TextEncryptor;

/**
 * EncryptionFacade facade
 * 
 * @author yizhou.xw
 * @version : EncryptionFacade.java, v 0.1 2021-07-26 9:37
 */
public interface EncryptionFacade {

    /**
     * encrypt by current user password
     * 
     * @param text original text
     * @param salt salt
     * @return encrypted password
     */
    String encryptByCurrentUserPassword(String text, String salt);

    /**
     * decrypt by current user password
     * 
     * @param encryptedText encrypted text
     * @param salt salt
     * @return original text
     */
    String decryptByCurrentUserPassword(String encryptedText, String salt);

    /**
     * generate salt
     * 
     * @return salt
     */
    String generateSalt();

    /**
     * get current user encryptor
     * 
     * @param salt
     * @return TextEncryptor
     */
    TextEncryptor currentUserEncryptor(String salt);

    /**
     * create encryptor by encryptionPassword
     * 
     * @param encryptionPassword
     * @param salt
     * @return TextEncryptor
     */
    TextEncryptor passwordEncryptor(String encryptionPassword, String salt);

    /**
     * create encryptor by userId
     *
     * @param userId
     * @param salt
     * @return TextEncryptor
     */
    TextEncryptor userEncryptor(Long userId, String salt);


    /**
     * create encryptor by organizationId
     *
     * @param organizationId
     * @param salt
     * @return TextEncryptor
     */
    TextEncryptor organizationEncryptor(Long organizationId, String salt);

}
