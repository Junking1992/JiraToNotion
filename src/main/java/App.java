import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 设置标题
        primaryStage.setTitle("JiraToNotion");
        // 设置窗口的图标
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon/icon.png")));
        // 加载主布局
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Home.fxml"));
        // 设置主场景
        primaryStage.setScene(new Scene(root, -1, -1));
        // 设定窗口无边框
        primaryStage.initStyle(StageStyle.DECORATED);
        // 设置窗口不能缩放
        primaryStage.setResizable(false);
        // 添加系统托盘
        addTrayIcon(primaryStage);
        primaryStage.show();
    }

    /**
     * 添加系统托盘
     */
    private void addTrayIcon(Stage primaryStage) throws AWTException {
        // 保证窗口关闭后Stage对象仍然存活
        Platform.setImplicitExit(false);

        // 图标路径
        URL url = getClass().getResource("/icon/icon.png");
        // 构建系统托盘图标
        java.awt.Image image = Toolkit.getDefaultToolkit().getImage(url);

        // 系统托盘弹出式菜单
        PopupMenu popup = new PopupMenu();
        // 设置菜单字体
        popup.setFont(new Font(null, 0, 18));
        // 菜单项
        MenuItem close = new MenuItem("Close");
        // 菜单项监听器
        close.addActionListener(e -> {
            System.exit(0);
        });
        // 菜单项添加至菜单中
        popup.add(close);

        // 系统托盘对象
        TrayIcon trayIcon = new TrayIcon(image, "JiraToNotion");
        // 托盘图标大小自动
        trayIcon.setImageAutoSize(true);
        // 鼠标操作事件监听
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 左键单击
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                    Platform.runLater(() -> {
                        // 是不是最小化
                        if (primaryStage.isIconified()) {
                            primaryStage.setIconified(false);
                        }
                        // 是不是展示状态
                        if (!primaryStage.isShowing()) {
                            primaryStage.show();
                        }
                        // 置顶
                        primaryStage.toFront();
                    });
                }
            }
        });
        // 添加弹出式菜单
        trayIcon.setPopupMenu(popup);

        // 添加托盘图标对象
        SystemTray.getSystemTray().add(trayIcon);
    }

    public static void main(String[] args) {
        launch(App.class, args);
    }
}
