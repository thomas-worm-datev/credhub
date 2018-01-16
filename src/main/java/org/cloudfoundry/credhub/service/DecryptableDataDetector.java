package org.cloudfoundry.credhub.service;

import org.cloudfoundry.credhub.data.CredentialVersionDataService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;

@Component
public class DecryptableDataDetector {

  private EncryptionKeyCanaryMapper encryptionKeyCanaryMapper;
  private CredentialVersionDataService credentialVersionDataService;

  DecryptableDataDetector(EncryptionKeyCanaryMapper encryptionKeyCanaryMapper,
                          CredentialVersionDataService credentialVersionDataService) {
    this.encryptionKeyCanaryMapper = encryptionKeyCanaryMapper;
    this.credentialVersionDataService = credentialVersionDataService;
  }

  public void check() {
    Collection<UUID> uuids = encryptionKeyCanaryMapper.getKnownCanaryUuids();

    Long countTotalCredentials = credentialVersionDataService.count();
    Long countCredentialsEncryptedWithKeyWeHave = credentialVersionDataService.countEncryptedWithKeyUuidIn(uuids);
    if (countTotalCredentials > 0 && countCredentialsEncryptedWithKeyWeHave == 0) {
      throw new RuntimeException(
          "The encryption keys provided cannot decrypt any of the " + countTotalCredentials
              + " value(s) in the database. "
              + "Please make sure you've provided the necessary encryption keys.");
    }
  }
}
