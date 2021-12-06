package service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

public class NotionApi {

    private HttpClient httpClient = new DefaultHttpClient();

    private Config config;

    public NotionApi(Config config) {
        this.config = config;
    }

    public void serch(){
        // 生成JSON
        JSONObject jsonObject = new JSONObject();
        JSONObject filter = new JSONObject();
        filter.put("value", "page");
        filter.put("property", "object");
        jsonObject.put("filter", filter);

        HttpRequest httpRequest = HttpRequest.post("https://api.notion.com/v1/search");
        httpRequest.header("Authorization", config.getNotionToken());
        httpRequest.header("Notion-Version", "2021-08-16");
        httpRequest.body(jsonObject.toJSONString());
        HttpResponse httpResponse = httpRequest.execute();
        System.out.println(httpResponse.body());
    }

    public void query() {
//        try {
//            // 生成JSON
//            JSONObject jsonObject = new JSONObject();
//            JSONObject filter = new JSONObject();
//            filter.put("value", "page");
//            filter.put("property", "object");
//            jsonObject.put("filter", filter);
//
//            System.out.println(jsonObject.toJSONString());
//
//            // POST请求
//            HttpPost httpPost = new HttpPost(new URI("https://api.notion.com/v1/search"));
//            httpPost.addHeader("Authorization", config.getNotionToken());
//            boolean hasVersion = config.getNotionVersion() != null && !config.getNotionVersion().trim().isEmpty();
//            httpPost.addHeader("Notion-Version", hasVersion ? config.getNotionVersion() : "2021-08-16");
//            StringEntity stringEntity = new StringEntity(jsonObject.toJSONString(), "UTF-8");
//            httpPost.setEntity(stringEntity);
//
//            // 执行请求
//            JSON response = request(httpPost);
//            System.out.println(response);
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        } catch (RestException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    /**
     * http请求
     */
//    private JSON request(HttpRequestBase req) throws Exception {
//        req.addHeader("Content-Type", "application/json;charset=utf-8");
//
//        HttpResponse resp = httpClient.execute(req);
//        HttpEntity ent = resp.getEntity();
//        StringBuilder result = new StringBuilder();
//
//        if (ent != null) {
//            String encoding = null;
//            if (ent.getContentEncoding() != null) {
//                encoding = ent.getContentEncoding().getValue();
//            }
//
//            if (encoding == null) {
//                Header contentTypeHeader = resp.getFirstHeader("Content-Type");
//                HeaderElement[] contentTypeElements = contentTypeHeader.getElements();
//                for (HeaderElement he : contentTypeElements) {
//                    NameValuePair nvp = he.getParameterByName("charset");
//                    if (nvp != null) {
//                        encoding = nvp.getValue();
//                    }
//                }
//            }
//
//            InputStreamReader isr = encoding != null ? new InputStreamReader(ent.getContent(), encoding) : new InputStreamReader(ent.getContent());
//            BufferedReader br = new BufferedReader(isr);
//            String line = "";
//
//            while ((line = br.readLine()) != null) {
//                result.append(line);
//            }
//        }
//
//        StatusLine sl = resp.getStatusLine();
//
//        if (sl.getStatusCode() >= 300) {
//            throw new Exception(sl.getReasonPhrase() + sl.getStatusCode() + result);
//        }
//
//        return result.length() > 0 ? JSON.parseObject(result.toString()) : null;
//    }
}
