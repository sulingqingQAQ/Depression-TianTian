package grainalcohol.dtt.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.architectury.platform.Platform;
import grainalcohol.dtt.DTTMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class DTTConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DTTConfig.class);
    private static final File CONFIG_DIR = new File(Platform.getConfigFolder().toFile(), DTTMod.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final DTTConfig INSTANCE = load();

    private ServerConfig serverConfig = new ServerConfig();
    private ClientConfig clientConfig = new ClientConfig();

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public static DTTConfig getInstance() {
        return INSTANCE;
    }

    private static DTTConfig load() {
        try {
            if (!CONFIG_DIR.exists()) {
                Files.createDirectories(CONFIG_DIR.toPath());
            }

            DTTConfig config = new DTTConfig();

            config.serverConfig = loadConfig(
                    new File(DTTConfig.CONFIG_DIR, "server.json"),
                    ServerConfig.class,
                    new ServerConfig()
            );
            config.clientConfig = loadConfig(
                    new File(DTTConfig.CONFIG_DIR, "client.json"),
                    ClientConfig.class,
                    new ClientConfig()
            );

            return config;
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory", e);
            return new DTTConfig();
        }
    }

    protected static <T> T loadConfig(File configFile, Class<T> configClass, T defaultConfig) {
        try {
            if (!CONFIG_DIR.exists()) {
                Files.createDirectories(CONFIG_DIR.toPath());
            }

            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    T loaded = GSON.fromJson(reader, configClass);
                    // 空文件、只有 null、或顶层不是 JSON 对象时 Gson 返回 null：
                    // 原来直接返回 null，首次读 getServerConfig().combatConfig... 就 NPE，这里报错并回退默认值
                    if (loaded == null) {
                        LOGGER.error("Config file {} is empty or not a JSON object; falling back to defaults", configFile.getName());
                        saveConfig(configFile, defaultConfig);
                        return defaultConfig;
                    }
                    return validate(configFile, loaded);
                } catch (IOException e) {
                    LOGGER.error("Failed to load config file: {}", configFile.getName(), e);
                } catch (JsonParseException e) {
                    // JSON 语法错误（或读取中被 Gson 包成 JsonIOException）：保留原文件（用户可能还要手工修），
                    // 本次用默认值继续跑，不让整个模组初始化失败
                    LOGGER.error("Config file {} could not be parsed or read ({}); falling back to defaults, the file is left untouched",
                            configFile.getName(), e.getMessage());
                    return defaultConfig;
                }
            }

            saveConfig(configFile, defaultConfig);
            return defaultConfig;
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory", e);
            return defaultConfig;
        }
    }

    /**
     * 反序列化成功之后的校验：分组字段为 null 时补默认分组，越界数值夹取到合法区间。<br>
     * {@link ServerConfig} 自带 validate；{@link ClientConfig} 不在本批改动范围内，这里就地补齐它的空分组。
     *
     * @param configFile 配置文件名，用于错误日志定位
     * @param loaded     反序列化得到的配置对象
     * @return 同一个对象（已就地校验/修复）
     */
    protected static <T> T validate(File configFile, T loaded) {
        String fileName = configFile.getName();
        if (loaded instanceof ServerConfig serverConfig) {
            serverConfig.validate(fileName);
        } else if (loaded instanceof ClientConfig clientConfig) {
            if (clientConfig.messageVariantConfig == null) {
                LOGGER.error("Config {}: field 'message_variant_config' is missing or null, falling back to default group", fileName);
                clientConfig.messageVariantConfig = new ClientConfig.MessageVariantConfig();
            }
            if (clientConfig.messageDisplayConfig == null) {
                LOGGER.error("Config {}: field 'message_display_config' is missing or null, falling back to default group", fileName);
                clientConfig.messageDisplayConfig = new ClientConfig.MessageDisplayConfig();
            }
            if (clientConfig.visualConfig == null) {
                LOGGER.error("Config {}: field 'visual_config' is missing or null, falling back to default group", fileName);
                clientConfig.visualConfig = new ClientConfig.VisualConfig();
            }
        }
        return loaded;
    }

    protected static <T> void saveConfig(File configFile, T config) {
        try {
            if (!CONFIG_DIR.exists()) {
                Files.createDirectories(CONFIG_DIR.toPath());
            }

            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(config, writer);
            } catch (IOException e) {
                LOGGER.error("Failed to save config file: {}", configFile.getName(), e);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory", e);
        }
    }
}
