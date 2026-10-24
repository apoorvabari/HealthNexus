package org.proj.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.firebase.config-path:}")
    private String configPath;

    @PostConstruct
    public void initializeFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            FirebaseOptions options;
            if (configPath != null && !configPath.trim().isEmpty()) {
                InputStream serviceAccount = new FileInputStream(configPath.trim());
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                logger.info("Initialized Firebase App with credentials file: {}", configPath);
            } else {
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .build();
                logger.info("Initialized Firebase App with Application Default Credentials");
            }
            FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            logger.warn("Could not initialize FirebaseApp with custom/default credentials: {}. Initializing dummy FirebaseApp fallback for verification.", e.getMessage());
            try {
                FirebaseOptions fallbackOptions = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .build();
                FirebaseApp.initializeApp(fallbackOptions);
            } catch (Exception ex) {
                logger.error("FirebaseApp initialization failed. Note: Firebase ID token verification requires valid Google credentials or config file.", ex.getMessage());
            }
        }
    }
}
