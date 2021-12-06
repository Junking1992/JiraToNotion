package service;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
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
                    long time = (currentTime - lastTime) / 1000;// / 60;
                    if (time < interval) {
                        continue;
                    }
                    lastTime = currentTime;
                }
                // 传输数据
                transmission();
                // 每10秒循环一次
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("运行已停止!");
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
        System.out.println(DateUtil.now() + "-正在传输数据!");
        jiraDataQuery();
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



    public void testNotionApi() {
        loadConfig();
        NotionApi notionApi = new NotionApi(config);

        // 查询接口测试
        JSONObject database = notionApi.serch("database");
        System.out.println(database);

        // 新增接口测试
        Properties.Title jTitle = new Properties.Title("J_Title", "Java");
        Properties.RichText jText = new Properties.RichText("J_Text", "Java");
        Properties.Number jNumber = new Properties.Number("J_Number", "999.99");
        Properties.Date jDate = new Properties.Date("J_Date", "2021-12-12");
        List<Properties> properties = new ArrayList<>();
        properties.add(jTitle);
        properties.add(jText);
        properties.add(jNumber);
        properties.add(jDate);
        boolean insert = notionApi.insert(properties);
        System.out.println(insert);

        // 修改接口测试
        // 删除接口测试
    }

    public static void main(String[] args) {
        JiraToNotion jiraToNotion = new JiraToNotion();
        jiraToNotion.testNotionApi();
    }
}
