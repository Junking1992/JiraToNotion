package notion;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import service.Config;

import java.util.List;

public class NotionApi {

    private Config config;

    public NotionApi(Config config) {
        this.config = config;
    }

    /**
     * 新增
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
        HttpResponse httpResponse = httpRequest.execute();

        if (httpResponse.getStatus() == 200) {
            return true;
        }
        return false;
    }

    /**
     * 查询
     */
    public JSONObject serch(String value) {
        // 生成JSON
        JSONObject jsonObject = new JSONObject();
        JSONObject filter = new JSONObject();
        filter.put("value", value);
        filter.put("property", "object");
        jsonObject.put("filter", filter);

        // POST请求
        HttpRequest httpRequest = HttpRequest.post("https://api.notion.com/v1/search");
        httpRequest.header("Authorization", config.getNotionToken());
        httpRequest.header("Notion-Version", config.getNotionVersion());
        httpRequest.body(jsonObject.toJSONString());
        httpRequest.timeout(60 * 1000);
        HttpResponse httpResponse = httpRequest.execute();

        return JSON.parseObject(httpResponse.body());
    }

}
