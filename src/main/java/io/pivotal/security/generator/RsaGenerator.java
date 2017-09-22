package io.pivotal.security.generator;

import io.pivotal.security.auth.UserContext;
import io.pivotal.security.credential.RsaCredentialValue;
import io.pivotal.security.request.GenerationParameters;
import io.pivotal.security.request.RsaGenerationParameters;
import io.pivotal.security.util.CertificateFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.KeyPair;

@Component
public class RsaGenerator implements CredentialGenerator<RsaCredentialValue> {

  private final LibcryptoRsaKeyPairGenerator keyGenerator;

  @Autowired
  RsaGenerator(LibcryptoRsaKeyPairGenerator keyGenerator) {
    this.keyGenerator = keyGenerator;
  }

  @Override
  public RsaCredentialValue generateCredential(GenerationParameters p, UserContext userContext) {
    RsaGenerationParameters params = (RsaGenerationParameters) p;
    try {
      final KeyPair keyPair = keyGenerator.generateKeyPair(params.getKeyLength());
      return new RsaCredentialValue(CertificateFormatter.pemOf(keyPair.getPublic()),
          CertificateFormatter.pemOf(keyPair.getPrivate()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
