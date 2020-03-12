package utilities;

import java.io.InputStream;
import java.util.Properties;

public class PropertyUtil {
    private static final Properties properties;

    private PropertyUtil() throws Exception {
    }

    static {
        properties = new Properties();
		try (InputStream input = PropertyUtil.class.getClassLoader().getResourceAsStream("config.properties")) {{
			properties.load(input);
		}
		} catch (Exception ex) {
			System.out.println(String.format("ファイルconfig.propertiesの読み込みに失敗しました。"));
		} finally {

		}
    }

    /**
     * プロパティ値を取得する
     *
     * @param key キー
     * @return 値
     */
    public static String getProperty(final String key) {
        return getProperty(key, "");
    }

    /**
     * プロパティ値を取得する
     *
     * @param key キー
     * @param defaultValue デフォルト値
     * @return キーが存在しない場合、デフォルト値
     *          存在する場合、値
     */
    public static String getProperty(final String key, final String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

}
