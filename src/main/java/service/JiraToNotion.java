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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
                        continue;
                    }
                    lastTime = currentTime;
                }
                // 传输数据
                try {
                    transmission();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 每10秒循环一次
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(DateUtil.now() + "运行已停止!");
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
            config = new Config(rootElement.element("jira").element("url").getText(), rootElement.element("jira").element("jql").getText(), rootElement.element("jira").element("username").getText(), rootElement.element("jira").element("password").getText(), rootElement.element("notion").element("token").getText(), rootElement.element("notion").element("databaseID").getText(), rootElement.element("notion").element("version").getText(), rootElement.element("run").element("interval").getText());
        } catch (Exception e) {
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
        System.out.println(DateUtil.now() + "开始传输!");
        // 查询Jira数据
        List<Issue> jiraList = jiraDataQuery();
        // 查询Notion数据
        List<String> notionList = notionDataQuery("Issue");
        // 新增或更新Notion的数据
        for (Issue issue : jiraList) {
            // 更新
            if (notionList.contains(issue.getKey())) {
                System.out.println(DateUtil.now() + "试图更新!");
            }
            // 新增
            else {
                insertIssue(issue);
            }
        }
        // 更新Notion标题时间
        new NotionApi(config).updateTitel("Jira任务（" + DateUtil.format(new Date(), "HH:mm") + "更新）");
        System.out.println(DateUtil.now() + "传输完成!");
    }

    /**
     * 查询jira数据
     */
    public List<Issue> jiraDataQuery() {
        JiraClient jiraClient = new JiraClient(config.getJiraUrl(), new BasicCredentials(config.getJiraUsername(), config.getJiraPassword()));
        try {
            Issue.SearchResult searchResult = jiraClient.searchIssues(config.getJiraJql());
            return searchResult.issues;
        } catch (JiraException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * 查询notion数据
     */
    public List<String> notionDataQuery(String titleName) {
        NotionApi notionApi = new NotionApi(config);
        JSONObject serch = notionApi.serch(NotionApi.PAGE, null);
        JSONArray results = serch.getJSONArray("results");
        while (serch.getBoolean("has_more")) {
            serch = notionApi.serch(NotionApi.PAGE, serch.getString("next_cursor"));
            results.addAll(serch.getJSONArray("results"));
        }

        // Title列表
        List<String> titleList = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            JSONObject jsonObject = results.getJSONObject(i);
            // 如果是删除状态则跳过
            if (jsonObject.getBoolean("archived")) {
                continue;
            }
            JSONObject properties = jsonObject.getJSONObject("properties");
            JSONObject title = properties.getJSONObject(titleName).getJSONArray("title").getJSONObject(0);
            titleList.add(title.getString("plain_text"));
        }
        return titleList;
    }

    /**
     * 新增Issue到Notion
     *
     * @param issue jira issue对象
     */
    public void insertIssue(Issue issue) {
        System.out.println(DateUtil.now() + "新增Issue:" + issue.getKey());
        List<Properties> properties = new ArrayList<>();
        properties.add(Properties.Title.create("Issue", issue.getKey()));
        properties.add(Properties.RichText.create("概要", issue.getSummary()));
        properties.add(Properties.RichText.create("项目", issue.getProject().getName()));
        properties.add(Properties.RichText.create("状态", issue.getStatus().getName()));
        properties.add(Properties.Url.create("Jira链接", config.getJiraUrl() + "/browse/" + issue.getKey()));
        List<Version> fixVersions = issue.getFixVersions();
        if (!fixVersions.isEmpty()) {
            properties.add(Properties.RichText.create("版本", fixVersions.get(0).getName()));
        }
        String created = (String) issue.getField("created");
        DateTime date = DateUtil.parse(created, DateUtil.newSimpleFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        String dateStr = DateUtil.format(date, "yyyy-MM-dd");
        properties.add(Properties.RichText.create("创建时间", dateStr));
        User reporter = issue.getReporter();
        if (reporter != null) {
            properties.add(Properties.RichText.create("报告人", reporter.getDisplayName()));
        }

        NotionApi notionApi = new NotionApi(config);
        notionApi.insert(properties);
    }

    public void updateIssue(Issue issue) {

    }
}
