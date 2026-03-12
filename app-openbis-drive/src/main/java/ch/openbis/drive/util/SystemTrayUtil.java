package ch.openbis.drive.util;

import ch.openbis.drive.gui.Launcher;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.model.Notification;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.text.Font;
import lombok.NonNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

public class SystemTrayUtil {

    SimpleBooleanProperty popupMenuVisible = new SimpleBooleanProperty(false);
    TrayIcon trayIcon;

    final I18n i18n;
    final @NonNull Function<Void, Void> stopCallback;


    Image normalIcon;
    Image exclamationMarkIcon;
    final JPopupMenu popupMenu;

    JButton notificationItem; // Renders better on Windows and Ubuntu
    MenuItem legacyNotificationItem; // Renders better on MAC-OS and is safer on other platforms

    static {
        initFonts();
    }

    public SystemTrayUtil(I18n i18n, @NonNull Function<Void, Void> stopCallback) {
        this.i18n = i18n;

        try {
            InputStream iconInputStream = Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(getNormalIconResourcePath()));
            this.normalIcon = ImageIO.read(iconInputStream).getScaledInstance(16,16, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            e.printStackTrace();
            this.normalIcon = getFallbackImage();
        }

        try {
            InputStream iconInputStream = Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(getExclamationMarkIconResourcePath()));
            this.exclamationMarkIcon = ImageIO.read(iconInputStream).getScaledInstance(16,16, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            e.printStackTrace();
            this.exclamationMarkIcon = getFallbackImage();
        }

        this.stopCallback = stopCallback;
        this.popupMenu = createPopupMenu(stopCallback);
    }

    private static String getNormalIconResourcePath() {
        return switch (OsDetectionUtil.detectOS()) {
            // Round icon not supported by AWT implementation for Linux environments
            case Linux -> "images/openbis-drive-icon-small.png";
            default -> "images/openbis-drive-icon-round-small.png";
        };
    }

    private static String getExclamationMarkIconResourcePath() {
        return switch (OsDetectionUtil.detectOS()) {
            // Round icon not supported by AWT implementation for Linux environments
            case Linux -> "images/openbis-drive-icon-small-excl-mark.png";
            default -> "images/openbis-drive-icon-round-small-excl-mark.png";
        };
    }

    /**
     * Add a system-tray icon for the application
     */
    public synchronized void addAppToTray() {
        if ( trayIcon == null ) {
            try {
                // make sure AWT toolkit is initialized
                java.awt.Toolkit.getDefaultToolkit();

                // return if there is no support
                if (!java.awt.SystemTray.isSupported()) {
                    System.out.println("No system tray support");
                    return;
                }

                // add system-tray icon
                java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
                java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(normalIcon);
                
                switch (OsDetectionUtil.detectOS()) {
                    case Linux, Windows -> {
                        // JSwing menu renders better than the plain awt.PopupMenu on Windows and Ubuntu,
                        // but it does not work well on MAC-OS
                        setUpJSwingMenuModernAlternative(trayIcon);
                    }
                    case Mac, Unknown -> {
                        trayIcon.setPopupMenu(createLegacyPopupMenu(stopCallback));
                    }
                }

                // add the application icon to the system-tray
                tray.add(trayIcon);
                this.trayIcon = trayIcon;
            } catch (java.awt.AWTException e) {
                System.out.println("Unable to init system tray");
                e.printStackTrace();
            }
        }
    }

    void setUpJSwingMenuModernAlternative(TrayIcon trayIcon) {
        final JFrame frame = new JFrame("");
        frame.setUndecorated(true);
        frame.setType(Window.Type.UTILITY);
        frame.setOpacity(0);
        frame.setBackground(new Color(0, 0, 0, 0));

        final JPanel jPanel = new JPanel();
        jPanel.setOpaque(false);
        jPanel.setBackground(new Color(0, 0, 0, 0));
        frame.add(jPanel);

        frame.setResizable(false);
        frame.setVisible(true);
        jPanel.setComponentPopupMenu(popupMenu);

        // if the user clicks on the tray icon, toggle the menu visibility
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point mouseClick = e.getLocationOnScreen();
                frame.setLocation(mouseClick);

                popupMenuVisible.set(!popupMenuVisible.get());
            }
        });

        popupMenuVisible.addListener( (obs, oldValue, newValue) -> {
            SwingUtilities.invokeLater( () -> {
                if (popupMenuVisible.get()) {
                    popupMenu.show(jPanel, 0, 0);
                    translatePopupMenuIfNecessary(frame.getLocation(), popupMenu, jPanel);
                } else {
                    popupMenu.setVisible(false);
                }
            });
        });
    }

    void translatePopupMenuIfNecessary(Point startingPoint, JPopupMenu popupMenu, JPanel popupParent) {
        GraphicsDevice[] graphicsDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        for ( GraphicsDevice graphicsDevice : graphicsDevices ) {
            Rectangle screenBounds = graphicsDevice.getDefaultConfiguration().getBounds();
            Point popupTranslation = new Point(0, 0);
            if ( screenBounds.contains(startingPoint) ) {
                if ( !screenBounds.contains(startingPoint.getX() + popupMenu.getWidth(), startingPoint.getY()) ) {
                    popupTranslation.translate(-popupMenu.getWidth() - 15, 0);
                }
                if ( !screenBounds.contains(startingPoint.getX(), startingPoint.getY() + popupMenu.getHeight()) ) {
                    popupTranslation.translate(0, -popupMenu.getHeight() - 15);
                }
                if ( !popupTranslation.equals(new Point(0, 0)) ) {
                    popupMenu.show(popupParent, (int)popupTranslation.getX(), (int)popupTranslation.getY());
                }
            }
        }
    }

    private JPopupMenu createPopupMenu(@NonNull Function<Void, Void> stopCallback) {
        final JPopupMenu popup = new RoundedPopupMenu();
        popup.setPopupSize(getPopupMenuWidthByLanguage(i18n.getLanguage()), 120);
        popup.setLayout(new GridLayout(4, 1));

        // Try to wake the GUI up
        JButton openItem = new RoundedMenuItem(i18n.get("system_tray.menu_item.open_panel"));
        openItem.setBackground(popup.getBackground());
        openItem.addActionListener(event -> {
            SwingUtilities.invokeLater(
                    () -> OpenBISDriveUtil.tryToAwakeOrStartGraphicalInterface(null)
            );
            SwingUtilities.invokeLater( () -> openItem.setBackground(popup.getBackground() ));
            popupMenuVisible.set(false);
        });

        // Try to wake the notification-section of the GUI up
        JButton notificationItem = new RoundedMenuItem(i18n.get("system_tray.menu_item.notifications"));
        notificationItem.setBackground(popup.getBackground());
        notificationItem.addActionListener(event -> {
            SwingUtilities.invokeLater(this::normalizeNotificationIndicators);
            SwingUtilities.invokeLater( () ->
                    OpenBISDriveUtil.tryToAwakeOrStartGraphicalInterface(OpenBISDriveUtil.GUISection.NOTIFICATIONS)
            );
            SwingUtilities.invokeLater( () -> notificationItem.setBackground(popup.getBackground() ));
            popupMenuVisible.set(false);
        });
        this.notificationItem = notificationItem;

        JPanel separatorSection = new JPanel();
        separatorSection.setLayout(new BoxLayout(separatorSection, BoxLayout.Y_AXIS));
        separatorSection.add(Box.createVerticalGlue());
        JSeparator separatorLine = new JSeparator();
        separatorLine.setForeground(popup.getBackground().darker());
        separatorSection.add(separatorLine);

        // Shut the background-service down, together with the graphical interface
        JButton exitItem = new RoundedMenuItem(i18n.get("system_tray.menu_item.stop_service"));
        exitItem.setBackground(popup.getBackground());
        exitItem.addActionListener(event -> {
            try {
                OpenBISDriveUtil.tryToStopGraphicalInterface();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopCallback.apply(null);
            }
        });

        popup.add(openItem);
        popup.add(notificationItem);
        popup.add(separatorSection);
        popup.add(exitItem);

        popup.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                popupMenuVisible.set(false);
            }
        });

        return popup;
    }

    private PopupMenu createLegacyPopupMenu(@NonNull Function<Void, Void> stopCallback) {
        final PopupMenu popup = new PopupMenu();

        // Try to wake the GUI up
        MenuItem openItem = new MenuItem(i18n.get("system_tray.menu_item.open_panel"));
        openItem.addActionListener(event -> {
            SwingUtilities.invokeLater(
                    () -> OpenBISDriveUtil.tryToAwakeOrStartGraphicalInterface(null)
            );
        });

        // Try to wake the notification-section of the GUI up
        MenuItem notificationItem = new MenuItem(i18n.get("system_tray.menu_item.notifications"));
        notificationItem.addActionListener(event -> {
            SwingUtilities.invokeLater(this::normalizeNotificationIndicators);
            SwingUtilities.invokeLater( () ->
                    OpenBISDriveUtil.tryToAwakeOrStartGraphicalInterface(OpenBISDriveUtil.GUISection.NOTIFICATIONS)
            );
        });
        this.legacyNotificationItem = notificationItem;

        // Shut the background-service down
        MenuItem exitItem = new MenuItem(i18n.get("system_tray.menu_item.stop_service"));
        exitItem.addActionListener(event -> {
            try {
                OpenBISDriveUtil.tryToStopGraphicalInterface();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopCallback.apply(null);
            }
        });

        popup.add(openItem);
        popup.add(notificationItem);
        popup.addSeparator();
        popup.add(exitItem);

        return popup;
    }

    synchronized public void raiseNotification(Notification notification) {
        SwingUtilities.invokeLater(this::turnNotificationIndicatorsOn);
    }

    synchronized public void turnNotificationIndicatorsOn() {
        try {
            if (trayIcon !=null && trayIcon.getImage() != exclamationMarkIcon) {
                this.trayIcon.setImage(exclamationMarkIcon);

                if ( notificationItem != null ) {
                    this.notificationItem.setText(i18n.get("system_tray.menu_item.new_notifications"));
                    this.notificationItem.setForeground(Color.WHITE);
                    this.notificationItem.setBackground(Color.RED);
                }

                if ( legacyNotificationItem != null ) {
                    this.legacyNotificationItem.setLabel(i18n.get("system_tray.menu_item.new_notifications"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized public void normalizeNotificationIndicators() {
        try {
            this.trayIcon.setImage(normalIcon);

            if ( notificationItem != null ) {
                this.notificationItem.setText(i18n.get("system_tray.menu_item.notifications"));
                this.notificationItem.setForeground(popupMenu.getForeground());
                this.notificationItem.setBackground(popupMenu.getBackground());
            }

            if ( legacyNotificationItem != null ) {
                this.legacyNotificationItem.setLabel(i18n.get("system_tray.menu_item.notifications"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Image getFallbackImage() {
        int width = 200;
        int height = 200;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        return img;
    }

    static int getPopupMenuWidthByLanguage( String languageTwoLetterCode ) {
        if (languageTwoLetterCode != null) {
            return switch (languageTwoLetterCode) {
                case "en" -> 170;
                case "it", "fr", "es" -> 190;
                case "de" -> 210;
                default -> 210;
            };
        } else {
            return 210;
        }
    }

    static void initFonts() {
        float systemFontSize = Toolkit.getToolkit().getFontLoader().getSystemFontSize();
        javafx.scene.text.Font.loadFont(Launcher.class.getClassLoader().getResourceAsStream("font/OpenSans.ttf"), systemFontSize);
        javafx.scene.text.Font.loadFont(Launcher.class.getClassLoader().getResourceAsStream("font/OpenSans-Bold.ttf"), systemFontSize);
        javafx.scene.text.Font.loadFont(Launcher.class.getClassLoader().getResourceAsStream("font/OpenSans-BoldItalic.ttf"), systemFontSize);
        javafx.scene.text.Font.loadFont(Launcher.class.getClassLoader().getResourceAsStream("font/OpenSans-Italic.ttf"), systemFontSize);
        Font.loadFont(Launcher.class.getClassLoader().getResourceAsStream("font/FontAwesome-7-Free-Solid-900.otf"), systemFontSize);
    }

    static class RoundedPopupMenu extends JPopupMenu {
        RoundedPopupMenu() {
            setLightWeightPopupEnabled(true);
            setOpaque(true);
            setBorder(new Border() {
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {

                }

                @Override
                public Insets getBorderInsets(Component c) {
                    return new Insets(10, 10, 10, 10);
                }

                @Override
                public boolean isBorderOpaque() {
                    return false;
                }
            });
        }

        @Override
        public void setVisible(boolean visible) {
            if (visible) {
                if (!isVisible()) {
                    super.setVisible(visible);

                    // attempt to set rounded corners
                    try {
                        Window w = SwingUtilities.getWindowAncestor(this);
                        w.setShape(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                super.setVisible(false);
            }
        }
    }

    static class RoundedMenuItem extends JButton implements MouseListener, MouseMotionListener {
        volatile Color effectiveBackgroundColor;

        public RoundedMenuItem(@NonNull String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);

            this.effectiveBackgroundColor = getBackground();

            addMouseListener(this);
            addMouseMotionListener(this);

            setFont(new java.awt.Font("Open Sans", java.awt.Font.PLAIN, 12));
        }

        @Override
        public void setBackground(Color bg) {
            super.setBackground(bg);
            this.effectiveBackgroundColor = bg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics2D = (Graphics2D) g;
            graphics2D.setColor(effectiveBackgroundColor);
            graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

            super.paintComponent(g);
        }

        //==============================================
        // Handle hover and click events
        //==============================================

        boolean isValidClickPosition(Point point) {
            //This could become more precise: taking the rounded corners into account
            return true;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (isValidClickPosition(e.getLocationOnScreen())) {
                effectiveBackgroundColor = getBackground().darker();
            }
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (isValidClickPosition(e.getLocationOnScreen())) {
                effectiveBackgroundColor = getBackground().darker();
            } else {
                effectiveBackgroundColor = getBackground();
            }
            repaint();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (isValidClickPosition(e.getLocationOnScreen())) {
                effectiveBackgroundColor = getBackground().darker();
            }
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            effectiveBackgroundColor = getBackground();
            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (isValidClickPosition(e.getLocationOnScreen())) {
                effectiveBackgroundColor = getBackground().darker();
            } else {
                effectiveBackgroundColor = getBackground();
            }
            repaint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mouseDragged(MouseEvent e) {

        }
    }
}
