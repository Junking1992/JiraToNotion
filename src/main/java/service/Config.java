package service;

/**
 * 用户配置
 */
public class Config {
    private String jiraUrl;
    private String jiraJql;
    private String jiraUsername;
    private String jiraPassword;
    private String notionToken;
    private String notionDatabaseId;
    private String interval;

    public Config(String jiraUrl, String jiraJql, String jiraUsername, String jiraPassword, String notionToken, String notionDatabaseId, String interval) {
        this.jiraUrl = jiraUrl;
        this.jiraJql = jiraJql;
        this.jiraUsername = jiraUsername;
        this.jiraPassword = jiraPassword;
        this.notionToken = notionToken;
        this.notionDatabaseId = notionDatabaseId;
        this.interval = interval;
    }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public String getJiraJql() {
        return jiraJql;
    }

    public String getJiraUsername() {
        return jiraUsername;
    }

    public String getJiraPassword() {
        return jiraPassword;
    }

    public String getNotionToken() {
        return notionToken;
    }

    public String getNotionDatabaseId() {
        return notionDatabaseId;
    }

    public String getInterval() {
        return interval;
    }
}
