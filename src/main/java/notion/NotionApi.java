package notion;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import service.Config;
import service.Log;

import java.util.List;

public class NotionApi {

    public static final String PAGE = "page";

    public static final String DATABASE = "database";

    private Config config;

    public NotionApi(Config config) {
        this.config = config;
    }

    /**
     * 新增
     *
     * @param properties 新增字段
     * @return 是否新增成功
     */
    public boolean insert(List<Properties> properties) {
        // 生成JSON
        JSONObject database_id = new JSONObject();
        database_id.put("database_id", config.getNotionDatabaseId());
        JSONObject propertiesObject = new JSONObject();
        for (Properties property : properties) {
            propertiesObject.put(property.getField(), property.toJsonObject());
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("parent", database_id);
        jsonObject.put("properties", propertiesObject);

        // POST请求
        HttpRequest httpRequest = HttpRequest.post("https://api.notion.com/v1/pages");
        httpRequest.header("Authorization", config.getNotionToken());
        httpRequest.header("Notion-Version", config.getNotionVersion());
        httpRequest.body(jsonObject.toJSONString());
        httpRequest.timeout(60 * 1000);
        HttpResponse httpResponse = httpExecute(httpRequest);
        if (httpResponse.getStatus() == 200) {
            return true;
        }
        return false;
    }

    /**
     * 删除
     *
     * @param pageId page_id
     * @return 是否删除成功
     */
    public boolean delete(String pageId) {
        // 生成JSON
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("archived", true);

        // PATCH请求
        HttpRequest httpRequest = HttpRequest.patch("https://api.notion.com/v1/pages/" + pageId);
        httpRequest.header("Authorization", config.getNotionToken());
        httpRequest.header("Notion-Version", config.getNotionVersion());
        httpRequest.body(jsonObject.toJSONString());
        httpRequest.timeout(60 * 1000);
        HttpResponse httpResponse = httpExecute(httpRequest);

        if (httpResponse.getStatus() == 200) {
            return true;
        }
        return false;
    }

    /**
     * 修改
     *
     * @param pageId     page_id
     * @param properties 需要修改的字段
     * @return 是否修改成功
     */
    public boolean update(String pageId, List<Properties> properties) {
        // 生成JSON
        JSONObject propertiesObject = new JSONObject();
        for (Properties property : properties) {
            propertiesObject.put(property.getField(), property.toJsonObject());
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("properties", propertiesObject);

        // PATCH请求
        HttpRequest httpRequest = HttpRequest.patch("https://api.notion.com/v1/pages/" + pageId);
        httpRequest.header("Authorization", config.getNotionToken());
        httpRequest.header("Notion-Version", config.getNotionVersion());
        httpRequest.body(jsonObject.toJSONString());
        httpRequest.timeout(60 * 1000);
        HttpResponse httpResponse = httpExecute(httpRequest);

        if (httpResponse.getStatus() == 200) {
            return true;
        }
        return false;
    }

    /**
     * 查询
     *
     * @param value       page or database
     * @param startCursor 游标
     * @return JSONObject
     */
    public JSONObject serch(String value, String startCursor) {
        // 生成JSON
        JSONObject jsonObject = new JSONObject();
        JSONObject filter = new JSONObject();
        filter.put("value", value);
        filter.put("property", "object");
        jsonObject.put("filter", filter);
        if (!StrUtil.isBlankIfStr(startCursor)) {
            jsonObject.put("start_cursor", startCursor);
        }

        // POST请求
        HttpRequest httpRequest = HttpRequest.post("https://api.notion.com/v1/search");
        httpRequest.header("Authorization", config.getNotionToken());
        httpRequest.header("Notion-Version", config.getNotionVersion());
        httpRequest.body(jsonObject.toJSONString());
        httpRequest.timeout(60 * 1000);
        HttpResponse httpResponse = httpExecute(httpRequest);
        return JSON.parseObject(httpResponse.body());
    }

    /**
     * 修改databases标题
     *
     * @param titel 标题
     * @return 是否修改成功
     */
    public boolean updateTitel(String titel) {
        // 生成JSON
        JSONObject textObject = new JSONObject();
        textObject.put("content", titel);
        JSONObject titleObject = new JSONObject();
        titleObject.put("text", textObject);
        JSONArray titleArray = new JSONArray();
        titleArray.add(titleObject);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", titleArray);

        HttpRequest httpRequest = HttpRequest.patch("https://api.notion.com/v1/databases/" + config.getNotionDatabaseId());
        httpRequest.header("Authorization", config.getNotionToken());
        httpRequest.header("Notion-Version", config.getNotionVersion());
        httpRequest.body(jsonObject.toJSONString());
        httpRequest.timeout(60 * 1000);
        HttpResponse httpResponse = httpExecute(httpRequest);
        if (httpResponse.getStatus() == 200) {
            return true;
        }
        return false;
    }

    /**
     * 发起请求,遇错重试
     *
     * @param httpRequest 请求
     * @return 响应
     */
    public HttpResponse httpExecute(HttpRequest httpRequest) {
        HttpResponse httpResponse = null;
        int i = 0;
        do {
            try {
                // 如果是重试就等待30s
                if (i > 0) {
                    Thread.sleep(30 * 1000);
                }
                i++;
                httpResponse = httpRequest.execute();
            } catch (Exception e) {
                Log.info("第" + i + "次请求" + httpRequest.getUrl() + "失败!");
                // 事不过三
                if (i >= 3) {
                    throw new RuntimeException(e);
                }
                Log.info("正尝试重新请求...");
            }
        } while (httpResponse == null);
        return httpResponse;
    }

}
