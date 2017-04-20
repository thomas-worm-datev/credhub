package io.pivotal.security.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pivotal.security.entity.NamedPasswordSecretData;
import io.pivotal.security.request.AccessControlEntry;
import io.pivotal.security.request.StringGenerationParameters;
import io.pivotal.security.service.Encryption;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PasswordCredential extends Credential<PasswordCredential> {

  private NamedPasswordSecretData delegate;
  private ObjectMapper objectMapper;
  private String password;

  public PasswordCredential(NamedPasswordSecretData delegate) {
    super(delegate);
    this.delegate = delegate;
    this.objectMapper = new ObjectMapper();
  }

  public PasswordCredential(String name) {
    this(new NamedPasswordSecretData(name));
  }

  public PasswordCredential() {
    this(new NamedPasswordSecretData());
  }

  public static PasswordCredential createNewVersion(
      PasswordCredential existing,
      String name,
      String password,
      StringGenerationParameters generationParameters,
      Encryptor encryptor,
      List<AccessControlEntry> accessControlEntries) {
    PasswordCredential secret;

    if (existing == null) {
      secret = new PasswordCredential(name);
    } else {
      secret = new PasswordCredential();
      secret.copyNameReferenceFrom(existing);
    }

    if (accessControlEntries == null) {
      accessControlEntries = new ArrayList<>();
    }

    secret.setAccessControlList(accessControlEntries);

    secret.setEncryptor(encryptor);
    secret.setPasswordAndGenerationParameters(password, generationParameters);
    return secret;
  }

  public String getPassword() {
    if (password == null) {
      password = encryptor.decrypt(
          delegate.getEncryptionKeyUuid(),
          delegate.getEncryptedValue(),
          delegate.getNonce()
      );
    }
    return password;
  }

  public PasswordCredential setPasswordAndGenerationParameters(String password,
                                                               StringGenerationParameters generationParameters) {
    if (password == null) {
      throw new IllegalArgumentException("password cannot be null");
    }

    try {
      String generationParameterJson =
          generationParameters != null ? objectMapper.writeValueAsString(generationParameters)
              : null;

      Encryption encryptedParameters = encryptor.encrypt(generationParameterJson);
      delegate.setEncryptedGenerationParameters(encryptedParameters.encryptedValue);
      delegate.setParametersNonce(encryptedParameters.nonce);

      Encryption encryptedPassword = encryptor.encrypt(password);
      delegate.setEncryptedValue(encryptedPassword.encryptedValue);
      delegate.setNonce(encryptedPassword.nonce);

      delegate.setEncryptionKeyUuid(encryptedPassword.canaryUuid);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public StringGenerationParameters getGenerationParameters() {
    String password = getPassword();
    Assert.notNull(password,
        "Password length generation parameter cannot be restored without an existing password");

    String parameterJson = encryptor.decrypt(
        delegate.getEncryptionKeyUuid(),
        delegate.getEncryptedGenerationParameters(),
        delegate.getParametersNonce()
    );

    if (parameterJson == null) {
      return null;
    }

    try {
      StringGenerationParameters passwordGenerationParameters = objectMapper
          .readValue(parameterJson, StringGenerationParameters.class);
      passwordGenerationParameters.setLength(password.length());
      return passwordGenerationParameters;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getSecretType() {
    return delegate.getSecretType();
  }

  public void rotate() {
    String decryptedPassword = this.getPassword();
    StringGenerationParameters decryptedGenerationParameters = this.getGenerationParameters();
    this.setPasswordAndGenerationParameters(decryptedPassword, decryptedGenerationParameters);
  }
}