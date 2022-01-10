package service;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.rcarz.jiraclient.*;
import notion.NotionApi;
import notion.Properties;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Jira数据传输至Notion
 */
public class JiraToNotion {

    private Path configPath = Paths.get(System.getProperty("user.home"), ".jiraToNotion", "config.xml");

    private Config config;

    private boolean run;

    private Long lastTime;

    /**
     * 运行
     */
    public void run() {
        new Thread(() -> {
            // 开始运行
            Log.info("开始运行!");
            run = true;
            while (run) {
                // 实时获取最新配置
                loadConfig();
                // 校验间隔时间
                String intervalStr = config.getInterval();
                if (intervalStr == null && intervalStr.trim().isEmpty()) {
                    throw new RuntimeException("间隔时间为空!");
                }
                Integer interval = Integer.valueOf(intervalStr);
                if (interval < 10) {
                    throw new RuntimeException("间隔时间不能小于10分钟!");
                }
                // 运行间隔时间控制
                if (lastTime == null) {
                    lastTime = System.currentTimeMillis();
                } else {
                    long currentTime = System.currentTimeMillis();
                    long time = (currentTime - lastTime) / 1000 / 60;
                    if (time < interval) {
                        // 每20秒循环一次
                        try {
                            Thread.sleep(20000);
                        } catch (Exception e) {
                            Log.error("间隔等待异常:", e);
                        }
                        continue;
                    }
                    lastTime = currentTime;
                }
                // 传输数据
                try {
                    transmission();
                } catch (Exception e) {
                    Log.error("传输数据异常:", e);
                }
            }
            Log.info("已停止运行!");
        }).start();
    }

    /**
     * 停止
     */
    public void stop() {
        run = false;
    }

    /**
     * 加载配置文件
     *
     * @return Config
     */
    public void loadConfig() {
        // 读取本地配置文件
        try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
            Document document = new SAXReader().read(bufferedReader);
            Element rootElement = document.getRootElement();
            config = new Config(rootElement.element("jira").element("url").getText(),
                    rootElement.element("jira").element("jql").getText(), rootElement.element("jira").element(
                            "username").getText(), rootElement.element("jira").element("password").getText(),
                    rootElement.element("notion").element("token").getText(), rootElement.element("notion").element(
                            "databaseID").getText(), rootElement.element("notion").element("version").getText(),
                    rootElement.element("run").element("interval").getText());
        } catch (Exception e) {
            Log.error("读取本地配置文件异常:", e);
        }
    }

    /**
     * 是否在运行
     */
    public boolean running() {
        return run;
    }

    /**
     * 传输数据
     */
    public void transmission() {
        Log.info("开始传输!");
        // 查询Jira数据
        List<Issue> jiraList = jiraDataQuery();
        // 查询Notion数据
        Map<String, Map<String, String>> notionMap = notionDataQuery("Issue");
        // Jira中所有issue号
        List<String> jiraIssueList = new ArrayList<>();
        // 记录本次传输情况
        int insert = 0;
        int update = 0;
        int delete = 0;
        // 新增或更新Notion的数据
        for (Issue issue : jiraList) {
            jiraIssueList.add(issue.getKey());
            // 更新
            if (notionMap.keySet().contains(issue.getKey())) {
                Map<String, String> subMap = notionMap.get(issue.getKey());
                // 校验是否需要更新
                String issueUpdateStr = (String) issue.getField("updated");
                String notionUpdateStr = subMap.get("updateTime");
                issueUpdateStr = DateUtil.parse(issueUpdateStr,
                        DateUtil.newSimpleFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).toString();
                // 两平台更新时间不相同的话就更新数据
                if (!issueUpdateStr.equals(notionUpdateStr)) {
                    update++;
                    updateIssue(subMap.get("id"), issue);
                }
            }
            // 新增
            else {
                insert++;
                insertIssue(issue);
            }
        }
        // 删除无效Issue
        for (String issueNo : notionMap.keySet()) {
            if (!jiraIssueList.contains(issueNo)) {
                delete++;
                Map<String, String> subMap = notionMap.get(issueNo);
                deleteIssue(subMap.get("id"), issueNo);
            }
        }
        // 更新Notion标题时间
        new NotionApi(config).updateTitel("Jira任务（" + DateUtil.format(new Date(), "HH:mm") + "更新）");
        Log.info("传输完成!新增:" + insert + "更新:" + update + "删除:" + delete);
    }

    /**
     * 查询jira数据
     */
    public List<Issue> jiraDataQuery() {
        JiraClient jiraClient = new JiraClient(config.getJiraUrl(), new BasicCredentials(config.getJiraUsername(),
                config.getJiraPassword()));
        try {
            Issue.SearchResult searchResult = jiraClient.searchIssues(config.getJiraJql());
            return searchResult.issues;
        } catch (JiraException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询notion数据
     */
    public Map<String, Map<String, String>> notionDataQuery(String titleName) {
        NotionApi notionApi = new NotionApi(config);
        JSONObject serch = notionApi.serch(NotionApi.PAGE, null);
        JSONArray results = serch.getJSONArray("results");
        while (serch.getBoolean("has_more")) {
            serch = notionApi.serch(NotionApi.PAGE, serch.getString("next_cursor"));
            results.addAll(serch.getJSONArray("results"));
        }

        Map<String, Map<String, String>> map = new HashMap<>();
        for (int i = 0; i < results.size(); i++) {
            JSONObject jsonObject = results.getJSONObject(i);
            // 如果是删除状态则跳过
            if (jsonObject.getBoolean("archived")) {
                continue;
            }
            JSONObject properties = jsonObject.getJSONObject("properties");
            JSONObject title = properties.getJSONObject(titleName).getJSONArray("title").getJSONObject(0);
            // Issue号
            String issue = title.getString("plain_text");
            // pageID
            String id = jsonObject.getString("id");
            // 更新时间
            String updateTime =
                    properties.getJSONObject("更新时间").getJSONArray("rich_text").getJSONObject(0).getJSONObject("text").getString("content");
            Map<String, String> subMap = new HashMap<>();
            subMap.put("id", id);
            subMap.put("updateTime", updateTime);
            // 重复数据自动删除
            if (map.keySet().contains(issue)) {
                deleteIssue(id, issue);
            } else {
                map.put(issue, subMap);
            }
        }
        return map;
    }

    /**
     * 新增Issue
     *
     * @param issue jira issue对象
     */
    public void insertIssue(Issue issue) {
        Log.info("新增Issue:" + issue.getKey());
        List<Properties> properties = getProperties(issue);
        NotionApi notionApi = new NotionApi(config);
        notionApi.insert(properties);
    }

    /**
     * 更新Issue
     *
     * @param pageId
     * @param issue  jira issue对象
     */
    public void updateIssue(String pageId, Issue issue) {
        Log.info("更新Issue:" + issue.getKey());
        List<Properties> properties = getProperties(issue);
        NotionApi notionApi = new NotionApi(config);
        notionApi.update(pageId, properties);
    }

    /**
     * 删除Issue
     *
     * @param pageId
     */
    public void deleteIssue(String pageId, String issueNo) {
        Log.info("删除Issue:" + issueNo);
        NotionApi notionApi = new NotionApi(config);
        notionApi.delete(pageId);
    }

    /**
     * 生成Notion中的字段
     */
    private List<Properties> getProperties(Issue issue) {
        List<Properties> properties = new ArrayList<>();
        properties.add(Properties.Title.create("Issue", issue.getKey()));
        properties.add(Properties.RichText.create("概要", issue.getSummary()));
        properties.add(Properties.RichText.create("项目", issue.getProject().getName()));
        properties.add(Properties.RichText.create("状态", issue.getStatus().getName()));
        properties.add(Properties.Url.create("Jira链接", config.getJiraUrl() + "/browse/" + issue.getKey()));
        List<Version> fixVersions = issue.getFixVersions();
        properties.add(Properties.RichText.create("版本", fixVersions.isEmpty() ? "未排版" : fixVersions.get(0).getName()));
        String created = (String) issue.getField("created");
        DateTime createdTime = DateUtil.parse(created, DateUtil.newSimpleFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        properties.add(Properties.RichText.create("创建时间", createdTime.toString()));
        String updated = (String) issue.getField("updated");
        DateTime updatedTime = DateUtil.parse(updated, DateUtil.newSimpleFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        properties.add(Properties.RichText.create("更新时间", updatedTime.toString()));
        User reporter = issue.getReporter();
        properties.add(Properties.RichText.create("报告人", reporter == null ? "离职员工" : reporter.getDisplayName()));
        return properties;
    }
}
