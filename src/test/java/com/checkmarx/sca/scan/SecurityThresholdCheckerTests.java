package com.checkmarx.sca.scan;

import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.configuration.ConfigurationEntry;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.checkmarx.sca.PropertiesConstants.*;
import static com.checkmarx.sca.configuration.ConfigurationEntry.SECURITY_RISK_THRESHOLD;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class SecurityThresholdCheckerTests {

    private Logger _logger;
    private Injector _injector;
    private Repositories _repositories;
    private PluginConfiguration _configuration;

    private HashMap<String, String> _properties;

    @BeforeEach
    public void beforeEach() {
        _logger = Mockito.mock(Logger.class);
        _repositories = Mockito.mock(Repositories.class);
        _configuration = Mockito.mock(PluginConfiguration.class);
        when(_configuration.getPropertyOrDefault(ConfigurationEntry.API_URL)).thenReturn(ConfigurationEntry.API_URL.defaultValue());

        var artifactRisksFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = new SecurityThresholdChecker(_repositories);

        var appInjector = new TestsInjector(_logger, _configuration, artifactRisksFiller, securityThresholdChecker);

        _injector = Guice.createInjector(appInjector);

        _properties = new HashMap<>();
    }

    @Test
    @DisplayName("Validate security risk threshold - without risks")
    public void validateSecurityRiskThresholdWithoutRisks() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("low");

        var repoPath = CreateRepoPath();
        MockGetProperty(repoPath, TOTAL_RISKS_COUNT, "0");
        MockGetProperty(repoPath, LOW_RISKS_COUNT, "0");
        MockGetProperty(repoPath, MEDIUM_RISKS_COUNT, "0");
        MockGetProperty(repoPath, HIGH_RISKS_COUNT, "0");
        MockGetProperty(repoPath, CRITICAL_RISKS_COUNT, "0");
        MockGetProperties(repoPath);

        securityThresholdChecker.checkSecurityRiskThreshold(repoPath, new ArrayList<>(List.of(repoPath)));

        loggerNeverCalled();
    }

    @Test
    @DisplayName("Validate security risk threshold - low risks")
    public void validateSecurityRiskThresholdLowRisks() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("medium");

        var repoPath = CreateRepoPath();
        MockGetProperty(repoPath, TOTAL_RISKS_COUNT, "15");
        MockGetProperty(repoPath, LOW_RISKS_COUNT, "15");
        MockGetProperty(repoPath, MEDIUM_RISKS_COUNT, "0");
        MockGetProperty(repoPath, HIGH_RISKS_COUNT, "0");
        MockGetProperty(repoPath, CRITICAL_RISKS_COUNT, "0");
        MockGetProperties(repoPath);

        securityThresholdChecker.checkSecurityRiskThreshold(repoPath, new ArrayList<>(List.of(repoPath)));

        loggerNeverCalled();
    }

    @Test
    @DisplayName("Validate security risk threshold - low risks blocked")
    public void validateSecurityRiskThresholdLowRisksBlocked() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("low");

        var repoPath = CreateRepoPath();
        MockGetProperty(repoPath, TOTAL_RISKS_COUNT, "15");
        MockGetProperty(repoPath, LOW_RISKS_COUNT, "15");
        MockGetProperty(repoPath, MEDIUM_RISKS_COUNT, "0");
        MockGetProperty(repoPath, HIGH_RISKS_COUNT, "0");
        MockGetProperty(repoPath, CRITICAL_RISKS_COUNT, "0");
        MockGetProperties(repoPath);

        var exception = Assertions.assertThrows(CancelException.class, () -> securityThresholdChecker.checkSecurityRiskThreshold(repoPath, new ArrayList<>(List.of(repoPath))));

        Assertions.assertEquals(exception.getErrorCode(), 403);

        loggerNeverCalled();
    }

    @Test
    @DisplayName("Validate security risk threshold - medium risks")
    public void validateSecurityRiskThresholdMediumRisks() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("Medium");

        var repoPath = CreateRepoPath();
        MockGetProperty(repoPath, TOTAL_RISKS_COUNT, "15");
        MockGetProperty(repoPath, LOW_RISKS_COUNT, "15");
        MockGetProperty(repoPath, MEDIUM_RISKS_COUNT, "0");
        MockGetProperty(repoPath, HIGH_RISKS_COUNT, "0");
        MockGetProperty(repoPath, CRITICAL_RISKS_COUNT, "0");
        MockGetProperties(repoPath);

        securityThresholdChecker.checkSecurityRiskThreshold(repoPath, new ArrayList<>(List.of(repoPath)));

        loggerNeverCalled();
    }

    @Test
    @DisplayName("Validate security risk threshold - medium risks blocked")
    public void validateSecurityRiskThresholdMediumRisksBlocked() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("medium");

        var repoPath = CreateRepoPath();
        MockGetProperty(repoPath, TOTAL_RISKS_COUNT, "15");
        MockGetProperty(repoPath, LOW_RISKS_COUNT, "0");
        MockGetProperty(repoPath, MEDIUM_RISKS_COUNT, "15");
        MockGetProperty(repoPath, HIGH_RISKS_COUNT, "0");
        MockGetProperty(repoPath, CRITICAL_RISKS_COUNT, "0");
        MockGetProperties(repoPath);

        var exception = Assertions.assertThrows(CancelException.class, () -> securityThresholdChecker.checkSecurityRiskThreshold(repoPath, new ArrayList<>(List.of(repoPath))));

        Assertions.assertEquals(exception.getErrorCode(), 403);

        loggerNeverCalled();
    }

    @Test
    @DisplayName("Validate security risk threshold - high risks")
    public void validateSecurityRiskThresholdHighRisks() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("high");

        var repoPath = CreateRepoPath();
        MockGetProperty(repoPath, TOTAL_RISKS_COUNT, "30");
        MockGetProperty(repoPath, LOW_RISKS_COUNT, "15");
        MockGetProperty(repoPath, MEDIUM_RISKS_COUNT, "15");
        MockGetProperty(repoPath, HIGH_RISKS_COUNT, "0");
        MockGetProperty(repoPath, CRITICAL_RISKS_COUNT, "0");
        MockGetProperties(repoPath);

        securityThresholdChecker.checkSecurityRiskThreshold(repoPath, new ArrayList<>(List.of(repoPath)));

        loggerNeverCalled();
    }

    @Test
    @DisplayName("Validate security risk threshold - high risks blocked")
    public void validateSecurityRiskThresholdHighRisksBlocked() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("high");

        var repoPath = CreateRepoPath();
        MockGetProperty(repoPath, TOTAL_RISKS_COUNT, "15");
        MockGetProperty(repoPath, LOW_RISKS_COUNT, "0");
        MockGetProperty(repoPath, MEDIUM_RISKS_COUNT, "0");
        MockGetProperty(repoPath, HIGH_RISKS_COUNT, "15");
        MockGetProperty(repoPath, CRITICAL_RISKS_COUNT, "0");
        MockGetProperties(repoPath);

        var exception = Assertions.assertThrows(CancelException.class, () -> securityThresholdChecker.checkSecurityRiskThreshold(repoPath, new ArrayList<>(List.of(repoPath))));

        Assertions.assertEquals(exception.getErrorCode(), 403);

        loggerNeverCalled();
    }

    @Test
    @DisplayName("Validate security risk threshold - critical risks")
    public void validateSecurityRiskThresholdCriticalRisks() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("critical");

        var repoPath = CreateRepoPath();
        MockGetProperty(repoPath, TOTAL_RISKS_COUNT, "45");
        MockGetProperty(repoPath, LOW_RISKS_COUNT, "15");
        MockGetProperty(repoPath, MEDIUM_RISKS_COUNT, "15");
        MockGetProperty(repoPath, HIGH_RISKS_COUNT, "15");
        MockGetProperty(repoPath, CRITICAL_RISKS_COUNT, "0");
        MockGetProperties(repoPath);

        securityThresholdChecker.checkSecurityRiskThreshold(repoPath, new ArrayList<>(List.of(repoPath)));

        loggerNeverCalled();
    }

    @Test
    @DisplayName("Validate security risk threshold - critical risks blocked")
    public void validateSecurityRiskThresholdCriticalRisksBlocked() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("critical");

        var repoPath = CreateRepoPath();
        MockGetProperty(repoPath, TOTAL_RISKS_COUNT, "15");
        MockGetProperty(repoPath, LOW_RISKS_COUNT, "0");
        MockGetProperty(repoPath, MEDIUM_RISKS_COUNT, "0");
        MockGetProperty(repoPath, HIGH_RISKS_COUNT, "0");
        MockGetProperty(repoPath, CRITICAL_RISKS_COUNT, "15");
        MockGetProperties(repoPath);

        var exception = Assertions.assertThrows(CancelException.class, () -> securityThresholdChecker.checkSecurityRiskThreshold(repoPath, new ArrayList<>(List.of(repoPath))));

        Assertions.assertEquals(exception.getErrorCode(), 403);

        loggerNeverCalled();
    }

    @Test
    @DisplayName("Validate security risk threshold - validation ignored")
    public void validateSecurityRiskThresholdValidationIgnored() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("low");

        var repoPath = CreateRepoPath();
        MockGetProperty(repoPath, IGNORE_THRESHOLD, "true");
        MockGetProperty(repoPath, TOTAL_RISKS_COUNT, "60");
        MockGetProperty(repoPath, LOW_RISKS_COUNT, "15");
        MockGetProperty(repoPath, MEDIUM_RISKS_COUNT, "15");
        MockGetProperty(repoPath, HIGH_RISKS_COUNT, "15");
        MockGetProperty(repoPath, CRITICAL_RISKS_COUNT, "15");
        MockGetProperties(repoPath);

        securityThresholdChecker.checkSecurityRiskThreshold(repoPath, new ArrayList<>(List.of(repoPath)));

        Mockito.verify(_logger, never()).error(isA(String.class));
        Mockito.verify(_logger, never()).error(isA(String.class), isA(Exception.class));

        Mockito.verify(_logger, times(1)).warn(Mockito.argThat(s -> s.contains(IGNORE_THRESHOLD)));
        Mockito.verify(_logger, never()).warn(isA(String.class), isA(Exception.class));
    }

    @Test
    @DisplayName("Validate security risk threshold - more than one RepoPath validation ignored")
    public void validateSecurityRiskThresholdMoreThanOneRepoPathValidationIgnored() {
        var securityThresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);

        SetSecurityRiskThreshold("low");

        var virtualPathLocal = CreateRepoPath();
        var repoPathLocal = CreateRepoPath();
        var repoPathRemote = CreateRepoPath();
        MockGetProperty(repoPathRemote, IGNORE_THRESHOLD, "true");
        MockGetProperty(repoPathRemote, TOTAL_RISKS_COUNT, "60");
        MockGetProperty(repoPathRemote, LOW_RISKS_COUNT, "15");
        MockGetProperty(repoPathRemote, MEDIUM_RISKS_COUNT, "15");
        MockGetProperty(repoPathRemote, HIGH_RISKS_COUNT, "15");
        MockGetProperty(repoPathRemote, CRITICAL_RISKS_COUNT, "15");
        MockGetProperties(repoPathLocal);
        MockGetProperties(repoPathRemote);

        securityThresholdChecker.checkSecurityRiskThreshold(virtualPathLocal, new ArrayList<>(List.of(repoPathLocal, repoPathRemote)));

        Mockito.verify(_logger, never()).error(isA(String.class));
        Mockito.verify(_logger, never()).error(isA(String.class), isA(Exception.class));

        Mockito.verify(_logger, times(1)).warn(Mockito.argThat(s -> s.contains("More than one RepoPath")));
        Mockito.verify(_logger, times(1)).warn(Mockito.argThat(s -> s.contains(IGNORE_THRESHOLD)));
        Mockito.verify(_logger, never()).warn(isA(String.class), isA(Exception.class));
    }

    private void loggerNeverCalled(){
        Mockito.verify(_logger, never()).error(isA(String.class));
        Mockito.verify(_logger, never()).error(isA(String.class), isA(Exception.class));

        Mockito.verify(_logger, never()).warn(isA(String.class));
        Mockito.verify(_logger, never()).warn(isA(String.class), isA(Exception.class));
    }

    private void SetSecurityRiskThreshold(String value){
        when(_configuration.getPropertyOrDefault(SECURITY_RISK_THRESHOLD)).thenReturn(value);
    }

    private void MockGetProperty(RepoPath repoPath, String key, String value){
        when(_repositories.getProperty(repoPath, key)).thenReturn(value);

        _properties.put(key, value);
    }

    private void MockGetProperties(RepoPath repoPath) {
        var properties = Mockito.mock(org.artifactory.md.Properties.class);
        when(properties.entries()).thenReturn(_properties.entrySet());
        when(_repositories.getProperties(repoPath)).thenReturn(properties);
    }

    private RepoPath CreateRepoPath(){
        var repoPath = Mockito.mock(RepoPath.class);

        when(repoPath.getName()).thenReturn(format("%s-%s", "lodash", "0.2.1"));

        return repoPath;
    }
}
