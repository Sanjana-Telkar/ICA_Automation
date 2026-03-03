import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import static java.awt.Font.ITALIC;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Main {

    // ===== USER EMAIL (remember across runs) =====
static volatile String USER_EMAIL = null;

// Small, durable key/value store (per user, per device)
static final java.util.prefs.Preferences PREFS =
        java.util.prefs.Preferences.userNodeForPackage(Main.class);
static final String PREF_KEY_USER_EMAIL = "userEmail";
    // ===== THEME =====
    static final Color BG_WHITE = new Color(246, 247, 251);
    static final Color CARD = Color.WHITE;
    static final Color BORDER = new Color(221, 224, 229);
    static final Color TEXT = new Color(25, 28, 33);
    static final Color ACCENT = new Color(64, 120, 255);
    static final Color ACCENT_DARK = new Color(52, 99, 218);
    static final Color MUTED = new Color(120, 126, 142);
    static final int RADIUS = 14;

    // gradient colors used by floating button and headers
    static final Color GRAD_START = new Color(255, 140, 160);
    static final Color GRAD_END   = new Color(150, 130, 255);

    // ===== SHARED FONTS =====
    static final String BASE_FONT = "Segoe UI";
    static final Font FONT_REGULAR = new Font(BASE_FONT, Font.PLAIN, 13);
    static final Font FONT_LARGE = new Font(BASE_FONT, Font.PLAIN, 15);
    static final Font FONT_BOLD = new Font(BASE_FONT, Font.BOLD, 16);
    static final Font FONT_SEMIBOLD = new Font(BASE_FONT, Font.PLAIN, 15);
    static final Font FONT_HINT = new Font(BASE_FONT, Font.PLAIN, 12);

    // Session timers
    static final long AUTO_CLOSE_MS = 2L * 60L * 60L * 1000L; // 2 hours
    static final long AUTO_RESET_MS = 3L * 60L * 60L * 1000L; // 3 hours

    // floater/dialog state
    static JFrame floatingFrame = null;
    static volatile boolean sessionOpen = false;
    static volatile boolean interactionOpen = false;
    static volatile JDialog activeDialog = null;
    static PromptSession currentSession = null;
    static volatile long lastSessionStartMs = 0L;

    // ===== COMPONENT HELPERS =====
    static Border roundedBorder(Color color) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        );
    }
    static Border focusBorder(boolean focused) {
        Color glow = focused ? new Color(64, 120, 255, 60) : new Color(0,0,0,0);
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(focused ? ACCENT : BORDER, 1, true),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(1, 1, 1, 1),
                        BorderFactory.createCompoundBorder(
                                new ShadowOutline(glow),
                                BorderFactory.createEmptyBorder(10, 12, 10, 12)
                        )
                )
        );
    }
    static void addFocusBorder(final JComponent c) {
        c.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { c.setBorder(focusBorder(true)); c.repaint(); }
            @Override public void focusLost(FocusEvent e)   { c.setBorder(focusBorder(false)); c.repaint(); }
        });
    }

    // Rounded button factory
    static JButton roundedButton(String label, Color bg, Color fg) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, w, h, 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(FONT_REGULAR);
        b.setForeground(fg);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    static JButton primaryButton(String label) {
        JButton b = roundedButton(label, ACCENT, Color.WHITE);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(ACCENT_DARK); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(ACCENT); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { b.setBackground(ACCENT_DARK.darker()); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e){ b.setBackground(ACCENT_DARK); }
        });
        return b;
    }
    static JButton ghostButton(String label) {
        JButton b = roundedButton(label, new Color(245, 246, 248), TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 232, 236), 1, true),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(new Color(238, 240, 243)); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(new Color(245, 246, 248)); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { b.setBackground(new Color(230, 232, 236)); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e){ b.setBackground(new Color(238, 240, 243)); }
        });
        return b;
    }
    static JButton ghostButtonArc(String label, int arc) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(FONT_REGULAR);
        b.setForeground(TEXT);
        b.setBackground(new Color(245, 246, 248));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 232, 236), 1, true),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(new Color(238, 240, 243)); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(new Color(245, 246, 248)); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { b.setBackground(new Color(230, 232, 236)); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e){ b.setBackground(new Color(238, 240, 243)); }
        });
        return b;
    }

    // ===== SIMPLE SHADOW PANEL WITH ROUNDED CORNERS =====
    static class ShadowPanel extends JPanel {
        private final int radius;
        ShadowPanel(LayoutManager lm, int radius) {
            super(lm);
            this.radius = radius;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, Math.max(0, w), Math.max(0, h), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
        @Override
        public Insets getInsets() { return new Insets(6, 6, 6, 6); }
    }

    // ===== OUTER SHADOW BORDER FOR FOCUS GLOW =====
    static class ShadowOutline implements Border {
        private final Color glow;
        ShadowOutline(Color glow) { this.glow = glow; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(glow);
            for (int i = 0; i < 3; i++) {
                g2.draw(new RoundRectangle2D.Float(x + i, y + i, width - i * 2 - 1, height - i * 2 - 1, RADIUS, RADIUS));
            }
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(6, 6, 6, 6); }
        @Override public boolean isBorderOpaque() { return false; }
    }

    // ===== ROUNDED BUBBLE PANEL =====
    static class RoundedBubble extends JPanel {
        private final JLabel label;
        RoundedBubble(String htmlText, Color bg, Color fg, Font font, int maxWidth) {
            super(new BorderLayout());
            setOpaque(false);
            label = new JLabel(htmlText);
            label.setFont(font);
            label.setForeground(fg);
            label.setSize(new Dimension(maxWidth, Short.MAX_VALUE));
            Dimension pref = label.getPreferredSize();
            int w = Math.min(pref.width + 24, maxWidth);
            int h = pref.height + 16;
            setPreferredSize(new Dimension(w, h));
            label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            add(label, BorderLayout.CENTER);
            label.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
            setBackground(bg);
        }
        @Override
        protected void paintComponent(Graphics g) {
            int arc = 18;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Dimension s = getSize();
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, Math.max(0, s.width), Math.max(0, s.height), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
        @Override
        public Dimension getPreferredSize() {
            Dimension d = label.getPreferredSize();
            return new Dimension(Math.min(Math.max(d.width + 24, 40), 520), d.height + 18);
        }
    }

    // ===== MODERN SCROLLBAR APPLIER =====
    static void applyModernScroll(JScrollPane scroll) {
        if (scroll == null) return;
        JScrollBar vsb = scroll.getVerticalScrollBar();
        vsb.setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
        vsb.setOpaque(false);
        vsb.setUnitIncrement(16);
        vsb.setBorder(null);
        vsb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            private final Color THUMB = new Color(80, 88, 110, 160);
            @Override protected void configureScrollBarColors() {
                this.thumbColor = THUMB;
                this.thumbDarkShadowColor = THUMB.darker();
                this.thumbHighlightColor = THUMB;
                this.trackColor = new Color(0,0,0,0);
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {}
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle tb) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(THUMB);
                int arc = 8;
                g2.fillRoundRect(tb.x, tb.y, tb.width, tb.height, arc, arc);
                g2.dispose();
            }
            @Override protected JButton createDecreaseButton(int orientation) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); b.setVisible(false); return b; }
            @Override protected JButton createIncreaseButton(int orientation) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); b.setVisible(false); return b; }
        });
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
    }

    // ===== SMOOTH SCROLL =====
    static void smoothScrollToBottom(JScrollPane scroll) {
        if (scroll == null) return;
        final JScrollBar vBar = scroll.getVerticalScrollBar();
        final int start = vBar.getValue();
        final int durationMs = 280;
        final int fps = 60;
        final int delay = Math.max(10, 1000 / fps);
        final long startTime = System.currentTimeMillis();
        final Timer t = new Timer(delay, null);
        t.addActionListener(e -> {
            final int targetNow = Math.max(0, vBar.getMaximum() - vBar.getVisibleAmount());
            float tNorm = (System.currentTimeMillis() - startTime) / (float) durationMs;
            if (tNorm >= 1f) { vBar.setValue(targetNow); t.stop(); return; }
            float p = 1 - (float)Math.pow(1 - tNorm, 3);
            int value = start + Math.round((targetNow - start) * p);
            vBar.setValue(value);
        });
        t.start();
    }

    // ===== COMBO RENDERER =====
    static class PlaceholderComboRenderer extends BasicComboBoxRenderer {
        private final int placeholderIndex;
        PlaceholderComboRenderer(int placeholderIndex) { this.placeholderIndex = placeholderIndex; }
        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setFont(FONT_REGULAR.deriveFont(14f));
            if (index == -1 && list.getSelectedIndex() == -1) {
                setText("— Select a collection —");
                setForeground(MUTED);
            } else if (index == placeholderIndex) {
                setForeground(MUTED);
            } else {
                setForeground(TEXT);
            }
            return this;
        }
    }

    // ===== ENHANCED MODERN COMBOBOX UI (Style C1) =====
    static class ModernComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton btn = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    g2.setColor(new Color(0,0,0,0));
                    g2.fillRect(0,0,w,h);
                    int size = 8;
                    int cx = w/2; int cy = h/2 + 1;
                    g2.setColor(new Color(120,126,142));
                    int[] xs = {cx - size/2, cx + size/2, cx};
                    int[] ys = {cy - size/4, cy - size/4, cy + size/3};
                    g2.fillPolygon(xs, ys, 3);
                    g2.dispose();
                }
                @Override public boolean isOpaque() { return false; }
            };
            btn.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 10));
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return btn;
        }
        @Override
        protected BasicComboPopup createPopup() {
            BasicComboPopup popup = new BasicComboPopup(comboBox) {
                @Override
                protected JScrollPane createScroller() {
                    JScrollPane sp = super.createScroller();
                    sp.setBorder(BorderFactory.createEmptyBorder());
                    applyModernScroll(sp);
                    return sp;
                }
                @Override
                public void show() {
                    super.show();
                    Component c = getList().getParent().getParent();
                    if (c instanceof JComponent jc) {
                        jc.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(221,224,229), 1, true),
                                BorderFactory.createEmptyBorder(4, 4, 4, 4)
                        ));
                    }
                }
            };
            return popup;
        }
        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            comboBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(221,224,229), 1, true),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)
            ));
            comboBox.setBackground(Color.WHITE);
            comboBox.setOpaque(true);
            comboBox.setFont(FONT_REGULAR.deriveFont(14f));
        }
    }

    // ===== CLIPBOARD =====
    public static String getClipboardText() {
        try {
            return (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
        } catch (HeadlessException | UnsupportedFlavorException | IOException e) {
            return "Could not read clipboard";
        }
    }
    public static void setClipboardText(String text) {
        try {
            StringSelection selection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        } catch (HeadlessException ignored) {}
    }
    private static String getClipboardTextSafe() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return (String) clipboard.getData(DataFlavor.stringFlavor);
            }
        } catch (Exception ignored) {}
        return "";
    }
    private static void clearClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection empty = new StringSelection("");
            clipboard.setContents(empty, null);
        } catch (Exception ignored) {}
    }

    // ===== UTILS =====
    public static void showError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    static void makeWindowRounded(Window w, int radius) {
        w.setBackground(new Color(0,0,0,0));
        w.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                w.setShape(new RoundRectangle2D.Double(0, 0, w.getWidth(), w.getHeight(), radius, radius));
            }
        });
    }
    static void positionDialogAboveFloating(Window dialog) {
        if (dialog == null) return;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int dw = dialog.getWidth(); int dh = dialog.getHeight();
        int edgeOffset = 12; int gapAboveFloating = 10;
        if (floatingFrame != null && floatingFrame.isShowing()) {
            Point fp = floatingFrame.getLocationOnScreen();
            int fx = fp.x, fy = fp.y, fw = floatingFrame.getWidth();
            int x = fx + fw - dw - edgeOffset;
            int y = fy - dh - gapAboveFloating;
            x = Math.max(8, Math.min(x, screen.width - dw - 8));
            y = Math.max(8, Math.min(y, screen.height - dh - 8));
            dialog.setLocation(x, y);
        } else {
            int x = screen.width - dw - 80 - edgeOffset;
            int y = screen.height - dh - 110;
            dialog.setLocation(Math.max(8, x), Math.max(8, y));
        }
    }

    // ================= DATA MODELS =================
    static class Message { final String text; final boolean isUser; Message(String t, boolean u){ text=t; isUser=u; } }
    static class Conversation {
        final String selectedCollection;
        final String title;
        final List<Message> messages = new ArrayList<>();
        boolean prepared = false;
        String lastAssistant = "";
        Conversation(String selectedCollection, String title) {
            this.selectedCollection = selectedCollection; this.title = title;
        }
        boolean hasAnyUserMessage() {
            for (Message m : messages) if (m.isUser) return true;
            return false;
        }
    }

    // ================= Shared Selenium Session (Login once) =================
    static class SharedBrowser {
        private static WebDriver driver = null;
        private static boolean loggedIn = false;

        private static final By CHAT_TEXTAREA = By.cssSelector(
                "textarea[data-testid='chat-input__textarea'], textarea[aria-label='Message']"
        );
        private static final By CONTEXT_CHIPS_LOCATOR = By.cssSelector(
            "[data-testid='composer-context-badge'], .cds--tag, .assistant-chip, [data-testid='composer-context-item']"
        );
        private static final By INSERT_OVERLAYS_LOCATOR = By.cssSelector(
            "div.insert-collection-container, div[role='dialog'], [data-modal-presented], .cds--modal, div[aria-modal='true']"
        );

        static synchronized void ensureReady() throws InterruptedException {
    if (driver != null && loggedIn) return;

    driver = new ChromeDriver();
    driver.get("https://remea.ica.ibm.com/ica/curatorai/apps/ui/new-chat/");
    driver.manage().window().maximize();
    driver.manage().window().minimize(); // start minimized to avoid disrupting user, will be closed after login

    // Resolve user email (prefer what the selection dialog captured)
    if (USER_EMAIL == null || USER_EMAIL.isBlank()) {
        String fromPrefs = null;
        try { fromPrefs = PREFS.get(PREF_KEY_USER_EMAIL, ""); } catch (Exception ignored) {}
        String sys = System.getProperty("userEmail");
        String env = System.getenv("USER_EMAIL");
        USER_EMAIL = (fromPrefs != null && !fromPrefs.isBlank()) ? fromPrefs
                   : (sys != null && !sys.isBlank()) ? sys
                   : (env != null && !env.isBlank()) ? env
                   : null;
    }
    if (USER_EMAIL == null || USER_EMAIL.isBlank()) {
        throw new IllegalStateException("Email not provided. Please start via the floating button and set your IBM ID in the Input Source window.");
    }

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

    // Login with provided email
    WebElement emailInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='text']"))
    );
    try { emailInput.clear(); } catch (Exception ignored) {}
    emailInput.sendKeys(USER_EMAIL);

    WebElement continueBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Continue']"))
    );
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", continueBtn);

    // Continue with your original flow
    WebElement arrow = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("span.ds-icon-arrow-right")
            )
    ).get(0);
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", arrow);

    WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(45));
    WebElement chat = longWait.until(
            ExpectedConditions.elementToBeClickable(
                    By.cssSelector("a[data-testid='home-start-chat']")
            )
    );
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", chat);

    // First-run tips best-effort
    try {
        WebElement closeBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[aria-label='Close']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeBtn);
    } catch (Exception ignored) {}

    loggedIn = true;
}

        static synchronized WebDriver getDriver() {
            if (driver == null) throw new IllegalStateException("Driver not initialized. Call ensureReady() first.");
            return driver;
        }

        private static int getContextCount(WebDriver d) {
            try { List<WebElement> chips = d.findElements(CONTEXT_CHIPS_LOCATOR);
                  return (chips == null) ? 0 : chips.size();
            } catch (Exception e) { return 0; }
        }
        private static void waitForOverlayToDisappear(WebDriver d, long timeoutSec) {
            new WebDriverWait(d, Duration.ofSeconds(timeoutSec)).until(dd -> {
                try {
                    List<WebElement> overlays = dd.findElements(INSERT_OVERLAYS_LOCATOR);
                    for (WebElement el : overlays) if (el.isDisplayed()) return false;
                    return true;
                } catch (StaleElementReferenceException ignored) { return true; }
                catch (Exception ignored) { return true; }
            });
        }
        private static boolean waitForContextAttached(WebDriver d, int baseCount, long timeoutSec) {
            try {
                new WebDriverWait(d, Duration.ofSeconds(timeoutSec)).until(dd -> getContextCount(dd) > baseCount);
                return true;
            } catch (TimeoutException e) {
                try {
                    List<WebElement> chips = d.findElements(CONTEXT_CHIPS_LOCATOR);
                    return chips != null && !chips.isEmpty();
                } catch (Exception ignored) {}
                return false;
            }
        }

        // In-app "Start a New Chat" + insert selected context (no re-login)
        static synchronized void startNewChat(String selectedCollection) throws InterruptedException {
            ensureReady();
            WebDriver d = driver;
            WebDriverWait wait = new WebDriverWait(d, Duration.ofSeconds(40));

            boolean started = false;
            try {
                WebElement newChatIcon = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.xpath("//span[contains(@class,'cds--tooltip-content') and text()='Start a New Chat']/ancestor::span//button")
                        )
                );
                ((JavascriptExecutor) d).executeScript("arguments[0].click();", newChatIcon);
                Thread.sleep(150);
                started = true;
            } catch (Exception e) {
                try {
                    WebElement homeStart = wait.until(
                        ExpectedConditions.elementToBeClickable(By.cssSelector("a[data-testid='home-start-chat']"))
                    );
                    ((JavascriptExecutor) d).executeScript("arguments[0].click();", homeStart);
                    started = true;
                } catch (Exception ignored) { }
            }

            try {
                WebElement gotItBtn = new WebDriverWait(d, Duration.ofSeconds(8)).until(
                        ExpectedConditions.elementToBeClickable(
                                By.cssSelector("button[data-testid='select-chat-type-popover-button']")
                        )
                );
                ((JavascriptExecutor) d).executeScript("arguments[0].click();", gotItBtn);
            } catch (Exception ignored) {}

            if (started) {
                try {
                    WebElement plusBtn = wait.until(
                            ExpectedConditions.elementToBeClickable(
                                    By.xpath("//button[.//*[name()='svg']//*[contains(@d,'17 15L17 8')]]")
                            )
                    );
                    ((JavascriptExecutor) d).executeScript("arguments[0].click();", plusBtn);
                    Thread.sleep(500);
                } catch (Exception ignored) {}
            }

            String mode = selectedCollection.split("::")[0];
            String cleanName = selectedCollection.split("::")[1];

            int baseContext = getContextCount(d);

            if (mode.equals("DOC")) {
                WebElement insertTarget = wait.until(
                        ExpectedConditions.elementToBeClickable(By.xpath("//div[text()='Insert Document Collection']"))
                );
                ((JavascriptExecutor) d).executeScript("arguments[0].click();", insertTarget);

                WebElement searchBox = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#Search_id"))
                );
                searchBox.clear(); searchBox.sendKeys(cleanName);

                WebElement addCollection = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.cssSelector("button[data-testid='insert-collection-tile-add-button']")
                        )
                );
                ((JavascriptExecutor) d).executeScript("arguments[0].click();", addCollection);

                waitForOverlayToDisappear(d, 15);
                waitForContextAttached(d, baseContext, 8);

            } else { // AGENT
                WebElement insertAgent = wait.until(
                        ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(text(),'Insert Assistant')]"))
                );
                ((JavascriptExecutor) d).executeScript("arguments[0].click();", insertAgent);

                WebElement agentSearch = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.cssSelector("input[data-testid='insert-agent-assistant-search-bar']")
                        )
                );
                agentSearch.clear(); agentSearch.sendKeys(cleanName);

                boolean added = false;
                try {
                    WebElement addBtn = new WebDriverWait(d, Duration.ofSeconds(6))
                            .until(ExpectedConditions.elementToBeClickable(
                                    By.cssSelector("button[data-testid='insert-agent-assistant-add-button'], button[data-testid='insert-agent-add-button']")));
                    ((JavascriptExecutor) d).executeScript("arguments[0].click();", addBtn);
                    added = true;
                } catch (Exception ignored) {}
                if (!added) {
                    try {
                        WebElement plusIcon = wait.until(
                                ExpectedConditions.elementToBeClickable(
                                        By.xpath("//*[name()='path' and contains(@d,'M16,2A14.1725')]/ancestor::button")
                                )
                        );
                        ((JavascriptExecutor) d).executeScript("arguments[0].click();", plusIcon);
                    } catch (Exception ignored) { }
                }

                waitForOverlayToDisappear(d, 15);
                waitForContextAttached(d, baseContext, 20);
            }

            new WebDriverWait(d, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(CHAT_TEXTAREA));
        }

        static synchronized void shutdown() {
            try { if (driver != null) driver.quit(); } catch (Exception ignored) {}
            driver = null; loggedIn = false;
        }
    }

    // ================= Answer capture helpers =================
    private static String assistantQueryJS() {
        return ""
        + "(() => {"
        + "  const sels = ["
        + "    'div.response-text-new-chat__container',"
        + "    'div.answer-with-markdown',"
        + "    'div.answer-text__paragraph',"
        + "    \"[data-testid='assistant-message']\","
        + "    \"[data-message-role='assistant']\","
        + "    'div.cds--markdown',"
        + "    'div.markdown-body'"
        + "  ];"
        + "  const nodes = [];"
        + "  sels.forEach(sel => { document.querySelectorAll(sel).forEach(n => nodes.push(n)); });"
        + "  const filtered = nodes.filter(n => !n.closest('.assistant-welcome-message'));"
        + "  const normalized = filtered.map(n => n.closest('div.response-text-new-chat__container') || n);"
        + "  const set = new Set();"
        + "  const uniq = [];"
        + "  normalized.forEach(el => { if(!set.has(el)){ set.add(el); uniq.push(el);} });"
        + "  return uniq;"
        + "})()";
    }
    private static void markExistingAssistantBlocks(WebDriver driver, String token) {
        String js =
            "var els = " + assistantQueryJS() + ";"
          + "els.forEach(el => el.setAttribute('data-ica-seen', arguments[0]));"
          + "return els.length;";
        ((JavascriptExecutor) driver).executeScript(js, token);
    }
    private static WebElement waitForNewAssistantBlock(WebDriver driver, String token, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            Object res = ((JavascriptExecutor) driver).executeScript(
                "var els = " + assistantQueryJS() + ";"
              + "for (var i = els.length - 1; i >= 0; i--) {"
              + "  var el = els[i];"
              + "  if (el.getAttribute('data-ica-seen') !== arguments[0]) {"
              + "    var txt = (el.innerText || '').trim();"
              + "    if (txt.length > 0) { return el; }"
              + "  }"
              + "}"
              + "return null;", token);
            if (res instanceof WebElement) {
                WebElement el = (WebElement) res;
                try { ((JavascriptExecutor) driver).executeScript("arguments[0].setAttribute('data-ica-seen', arguments[1]);", el, token); } catch (Exception ignored) {}
                return el;
            }
            Thread.sleep(250);
        }
        return null;
    }

    // ===== Send helpers =====
    private static final By CHAT_TEXTAREA = By.cssSelector(
        "textarea[data-testid='chat-input__textarea'], textarea[aria-label='Message']"
    );
    private static final By SEND_BTN_EXACT = By.cssSelector(
        "button[data-testid='send-prompt-container-send-button']"
    );
    private static final By[] SEND_BUTTON_FALLBACKS = new By[] {
        By.cssSelector("button[aria-label='Send']"),
        By.cssSelector("button[aria-label*='Send']"),
        By.cssSelector("button[title='Send']"),
        By.cssSelector("button.cds--btn--primary"),
        By.xpath("//button[.//span[contains(.,'Send')]]"),
        By.xpath("//button[.//*[name()='svg' and (@aria-label='Send' or @data-icon='send')]]")
    };
    private static WebElement locateChatInput(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(CHAT_TEXTAREA));
    }
    private static void setTextAndDispatch(WebDriver driver, WebElement textarea, String value) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "const el=arguments[0], val=arguments[1];" +
                "el.focus();" +
                "el.value=val;" +
                "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                "el.dispatchEvent(new Event('change',{bubbles:true}));",
                textarea, value
            );
        } catch (Exception ignored) {}
    }
    private static WebElement findComposerRoot(WebElement chatInput) {
        try {
            return chatInput.findElement(By.xpath(
                "ancestor::*[.//button[@data-testid='send-prompt-container-send-button']][1]"
            ));
        } catch (Exception ignored) {}
        try { return chatInput.findElement(By.xpath("ancestor::div[1]")); } catch (Exception ignored) {}
        return null;
    }
    private static boolean clickSendButtonScoped(WebDriver driver, WebElement chatInput, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        WebElement scope = findComposerRoot(chatInput);

        while (System.currentTimeMillis() < end) {
            try {
                if (scope != null) {
                    List<WebElement> exactScoped = scope.findElements(SEND_BTN_EXACT);
                    for (int i = exactScoped.size()-1; i >= 0; i--) {
                        WebElement b = exactScoped.get(i);
                        if (!b.isDisplayed()) continue;
                        String dis = b.getAttribute("disabled");
                        String aria = b.getAttribute("aria-disabled");
                        if ("true".equalsIgnoreCase(dis) || "true".equalsIgnoreCase(aria)) continue;
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'})", b);
                        try { new WebDriverWait(driver, Duration.ofSeconds(2)).until(ExpectedConditions.elementToBeClickable(b)); } catch (Exception ignored) {}
                        try { new Actions(driver).moveToElement(b).pause(java.time.Duration.ofMillis(120)).click().perform(); return true; }
                        catch (Exception e1) { try { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", b); return true; } catch (Exception ignored) {} }
                    }
                }
                List<WebElement> exactGlobal = driver.findElements(SEND_BTN_EXACT);
                for (int i = exactGlobal.size()-1; i >= 0; i--) {
                    WebElement b = exactGlobal.get(i);
                    if (!b.isDisplayed()) continue;
                    String dis = b.getAttribute("disabled");
                    String aria = b.getAttribute("aria-disabled");
                    if ("true".equalsIgnoreCase(dis) || "true".equalsIgnoreCase(aria)) continue;
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'})", b);
                    try { new WebDriverWait(driver, Duration.ofSeconds(2)).until(ExpectedConditions.elementToBeClickable(b)); } catch (Exception ignored) {}
                    try { new Actions(driver).moveToElement(b).pause(java.time.Duration.ofMillis(120)).click().perform(); return true; }
                    catch (Exception e1) { try { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", b); return true; } catch (Exception ignored) {} }
                }
                if (scope != null) {
                    for (By by : SEND_BUTTON_FALLBACKS) {
                        List<WebElement> cands = scope.findElements(by);
                        for (int i = cands.size()-1; i >= 0; i--) {
                            WebElement b = cands.get(i);
                            if (!b.isDisplayed()) continue;
                            String dis = b.getAttribute("disabled");
                            String aria = b.getAttribute("aria-disabled");
                            if ("true".equalsIgnoreCase(dis) || "true".equalsIgnoreCase(aria)) continue;
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'})", b);
                            try { new WebDriverWait(driver, Duration.ofSeconds(2)).until(ExpectedConditions.elementToBeClickable(b)); } catch (Exception ignored) {}
                            try { new Actions(driver).moveToElement(b).pause(java.time.Duration.ofMillis(120)).click().perform(); return true; }
                            catch (Exception e1) { try { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", b); return true; } catch (Exception ignored) {} }
                        }
                    }
                }
            } catch (Exception ignored) {}
            try { Thread.sleep(120); } catch (InterruptedException ignored) {}
        }
        return false;
    }
    private static String jsCopyNode(WebDriver driver, WebElement node) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "window.__copiedText='';" +
                "try{" +
                "  var r=document.createRange();" +
                "  r.selectNode(arguments[0]);" +
                "  var s=window.getSelection();" +
                "  s.removeAllRanges();" +
                "  s.addRange(r);" +
                "  try{document.execCommand('copy');}catch(e){}" +
                "  window.__copiedText=s.toString();" +
                "}catch(err){window.__copiedText='';}", node
            );
            Object got = ((JavascriptExecutor) driver).executeScript("return window.__copiedText || '';");
            return (got == null) ? "" : got.toString();
        } catch (Exception e) { return ""; }
    }
    private static String smartExtractCleanText(WebDriver driver, WebElement container) {
        StringBuilder out = new StringBuilder();
        List<WebElement> blocks = container.findElements(By.cssSelector("h1, h2, h3, p, ol, ul, li, div.answer-text__paragraph, div.answer-with-markdown"));
        for (WebElement b : blocks) {
            String tag = b.getTagName().toLowerCase();
            String text = (b.getText() == null) ? "" : b.getText().trim();
            if (text.isEmpty()) continue;
            List<WebElement> links = new ArrayList<>();
            try { links = b.findElements(By.tagName("a")); } catch (Exception ignored) {}
            for (WebElement a : links) {
                try {
                    String linkText = a.getText();
                    String href = a.getAttribute("href");
                    if (href != null && !href.isEmpty()) text = text.replace(linkText, linkText + " (" + href + ")");
                } catch (Exception ignored) {}
            }
            if ("li".equals(tag)) out.append("• ").append(text).append("\n");
            else if (tag.matches("h[1-3]")) {
                out.append(text).append("\n");
                out.append("-".repeat(Math.max(3, Math.min(60, text.length())))).append("\n\n");
            } else out.append(text).append("\n\n");
        }
        String s = out.toString().trim();
        if (s.isEmpty()) { try { s = container.getText().trim(); } catch (Exception ignored) {} }
        return (s == null) ? "" : s;
    }
    private static String tryExtractAnswer(WebDriver driver, WebElement node) throws InterruptedException {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "if(!window.__copyHookInstalled){window.__copyHookInstalled=true;window.__copiedText='';" +
                "document.addEventListener('copy',function(e){try{" +
                "var t=e.clipboardData.getData('text/plain');if(t){window.__copiedText=t;}" +
                "}catch(err){}},true);}"
            );
        } catch (Exception ignored) {}

        List<WebElement> localCopyBtns = new ArrayList<>();
        try { localCopyBtns = node.findElements(By.cssSelector("button[data-testid='copy-answer']")); } catch (Exception ignored) {}
        String copied = "";
        if (!localCopyBtns.isEmpty()) {
            WebElement copyBtnEl = localCopyBtns.get(localCopyBtns.size() - 1);
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(copyBtnEl));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'})", copyBtnEl);

            Thread.sleep(800);

            String prevClip = getClipboardTextSafe(); clearClipboard();
            try { new Actions(driver).moveToElement(copyBtnEl).pause(java.time.Duration.ofMillis(120)).click().perform(); }
            catch (Exception e) { try { copyBtnEl.click(); } catch (Exception ignored) {} }
            long startPoll = System.currentTimeMillis();
            while (System.currentTimeMillis() - startPoll < 9000) {
                try {
                    String hook = (String)((JavascriptExecutor) driver).executeScript("return window.__copiedText||'';");
                    if (hook != null && !hook.trim().isEmpty()) { copied = hook; break; }
                } catch (Exception ignored) {}
                String osClip = getClipboardTextSafe();
                if (osClip != null && !osClip.trim().isEmpty() && !osClip.equals(prevClip)) { copied = osClip; break; }
                Thread.sleep(250);
            }
        }
        if (copied == null || copied.trim().isEmpty()) copied = jsCopyNode(driver, node);
        if (copied == null || copied.trim().isEmpty()) copied = smartExtractCleanText(driver, node);
        setClipboardText(copied == null ? "" : copied);
        return copied;
    }

    // ===== Placeholder-enabled TextArea =====
    static class HintTextArea extends JTextArea {
        private String hint = "";
        public void setHint(String hint) { this.hint = hint; repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner() && hint != null && !hint.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(130, 138, 150));
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                Insets ins = getInsets();
                g2.drawString(hint, ins.left + 4, ins.top + g2.getFontMetrics().getAscent() + 1);
                g2.dispose();
            }
        }
    }

    // ===== Typing indicator (three jumping dots) =====
    static class TypingIndicator extends JPanel {
        private final JLabel label = new JLabel("");
        private final Timer timer;
        private int tick = 0;
        TypingIndicator() {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
            setBorder(BorderFactory.createEmptyBorder(6,0,0,0));
            JLabel text = new JLabel("Assistant is typing");
            text.setFont(FONT_REGULAR.deriveFont(ITALIC,14F));
            Color tint = new Color(100, 108, 125);
            text.setForeground(tint);
            label.setFont(FONT_BOLD.deriveFont(ITALIC, 16F));
            label.setForeground(tint);
            add(text); add(label);
            timer = new Timer(300, e -> {
                tick = (tick + 1) % 4;
                label.setText((tick == 0 ? "" : ".".repeat(tick)));
            });
            timer.start();
        }
        void stop() { timer.stop(); }
    }

    // ================= PromptSession (multi-page chat) =================
    static class PromptSession {
        public final JDialog dialog;
        final JButton backBtn, copyBtn, closeBtn;
        JButton newChatBtn, prevBtn, nextBtn, sendBtn;
        HintTextArea inputArea;
        JLabel headerCollectionLabel;
        JPanel messagesPanel;
        JScrollPane messagesScroll;

        JPanel typingHost;
        TypingIndicator typingIndicator;

        JPanel inputBarPanel;
        JPanel actionsLeftGroup;
        JPanel actionsRightGroup;
        JPanel actionsRow;
        JScrollPane inputScrollPane;

        final List<Conversation> conversations = new ArrayList<>();
        int currentConversationIndex = 0;

        private Runnable disableSend, enableSend;

        // NEW: controls when header New Chat appears
        boolean newChatUnlocked = false;

        PromptSession(JDialog dialog, JButton backBtn, JButton copyBtn, JButton closeBtn) {
            this.dialog = dialog; this.backBtn = backBtn; this.copyBtn = copyBtn; this.closeBtn = closeBtn;
        }

        Conversation currentConversation() {
            if (conversations.isEmpty()) return null;
            return conversations.get(currentConversationIndex);
        }

        void updateHeaderForCurrent() {
            Conversation c = currentConversation();
            if (c == null) return;

            if (headerCollectionLabel != null) {
                headerCollectionLabel.setText(c.selectedCollection + "  —  " + c.title);
            }

            boolean isLatest = currentConversationIndex == conversations.size() - 1;
            boolean hasPrev  = currentConversationIndex > 0;
            boolean hasNext  = currentConversationIndex < conversations.size() - 1;

            if (sendBtn != null)          sendBtn.setVisible(isLatest);
            if (inputArea != null)        inputArea.setVisible(isLatest);
            if (inputBarPanel != null)    inputBarPanel.setVisible(isLatest);
            if (typingHost != null)       typingHost.setVisible(isLatest);
            if (!isLatest)                hideTyping();

            if (copyBtn != null) {
                copyBtn.setVisible(isLatest && copyBtn.isEnabled());
            }
            // New Chat visible only on latest AND when unlocked
            if (newChatBtn != null) {
                newChatBtn.setVisible(isLatest && newChatUnlocked);
            }

            if (backBtn != null) {
                if (c.hasAnyUserMessage()) {
                    backBtn.setVisible(false);
                } else {
                    backBtn.setVisible(isLatest);
                    backBtn.setEnabled(isLatest);
                }
            }

            if (prevBtn != null) prevBtn.setVisible(hasPrev);
            if (nextBtn != null) nextBtn.setVisible(hasNext);

            if (actionsLeftGroup != null)   actionsLeftGroup.setVisible(hasPrev || hasNext);
            if (actionsRightGroup != null)  actionsRightGroup.setVisible(isLatest);

            if (actionsRow != null) {
                boolean anyVisible =
                    (actionsLeftGroup != null && actionsLeftGroup.isVisible()) ||
                    (actionsRightGroup != null && actionsRightGroup.isVisible());
                actionsRow.setVisible(anyVisible);
            }

            if (sendBtn != null)   sendBtn.setEnabled(isLatest);
            if (inputArea != null) inputArea.setEnabled(isLatest);
            if (copyBtn != null)   copyBtn.setEnabled(isLatest && copyBtn.isEnabled());

            if (dialog != null) { dialog.revalidate(); dialog.repaint(); }
        }

        void clearMessagesUI() {
            if (messagesPanel == null) return;
            messagesPanel.removeAll();
            messagesPanel.revalidate(); messagesPanel.repaint();
        }
        void addBubbleToUI(String text, boolean isUser) {
            if (text == null || text.trim().isEmpty()) return;
            String html = "<html><div style='max-width:auto;'>" + text.replace("\n", "<br>") + "</div></html>";
            int bubbleMax = 380;

            RoundedBubble bubble = new RoundedBubble(html,
                    isUser ? GRAD_END : new Color(245, 246, 248),
                    isUser ? Color.WHITE : TEXT,
                    FONT_LARGE.deriveFont(14f),
                    bubbleMax);

            JPanel row = new JPanel(new FlowLayout(isUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 2));
            row.setOpaque(false);
            row.add(bubble);
            messagesPanel.add(Box.createVerticalStrut(10));
            messagesPanel.add(row);
            messagesPanel.revalidate();
            SwingUtilities.invokeLater(() -> smoothScrollToBottom(messagesScroll));
        }
        void addMessage(String text, boolean isUser) {
            Conversation c = currentConversation();
            if (c == null) return;
            c.messages.add(new Message(text, isUser));
            addBubbleToUI(text, isUser);

            if (isUser && backBtn != null && backBtn.isVisible()) {
                SwingUtilities.invokeLater(() -> backBtn.setVisible(false));
            }
        }
        void renderConversation(int index) {
            if (index < 0 || index >= conversations.size()) return;
            currentConversationIndex = index;
            clearMessagesUI();
            Conversation c = currentConversation();
            if (c != null) for (Message m : c.messages) addBubbleToUI(m.text, m.isUser);
            updateHeaderForCurrent();
            SwingUtilities.invokeLater(() -> smoothScrollToBottom(messagesScroll));
        }

        void showTyping() {
            if (typingIndicator != null) return;
            typingIndicator = new TypingIndicator();
            if (typingHost != null) {
                typingHost.removeAll();
                typingHost.add(typingIndicator);
                typingHost.revalidate();
                typingHost.repaint();
            }
        }
        void hideTyping() {
            if (typingIndicator == null) return;
            typingIndicator.stop();
            if (typingHost != null) {
                typingHost.removeAll();
                typingHost.revalidate();
                typingHost.repaint();
            }
            typingIndicator = null;
        }

        void onAssistantResponse(String assistantText) {
            hideTyping();
            if (assistantText == null) return;
            Conversation c = currentConversation();
            if (c != null) c.lastAssistant = assistantText;

            addMessage(assistantText, false);

            if (!copyBtn.isVisible()) { copyBtn.setVisible(true); copyBtn.setEnabled(true); }

            try {
                dialog.setAlwaysOnTop(true);
                dialog.toFront();
                dialog.requestFocus();
                final Timer t = new Timer(120, evt -> dialog.setAlwaysOnTop(false));
                t.setRepeats(false); t.start();
            } catch (SecurityException ignored) {}

            dialog.revalidate(); dialog.repaint();
        }

        public void close() {
            SwingUtilities.invokeLater(() -> {
                try { dialog.dispose(); } catch (Exception ignored) {}
                SharedBrowser.shutdown();
                sessionOpen = false; currentSession = null; activeDialog = null; interactionOpen = false;
            });
        }

        public void setSendControls(Runnable disable, Runnable enable) {
            this.disableSend = disable; this.enableSend = enable;
        }
        public void setSending(boolean busy) {
            if (busy) { if (disableSend != null) disableSend.run(); showTyping(); }
            else      { if (enableSend != null)  enableSend.run();  hideTyping(); }
        }
    }

    // ================= Floating controller =================
    public static void createFloatingController() {
        JFrame f = new JFrame();
        floatingFrame = f;
        f.setSize(50, 50);
        f.setUndecorated(true);
        f.setAlwaysOnTop(true);
        f.setBackground(new Color(0,0,0,0));

        JButton run = new JButton("") {
            boolean hover = false;
            { setFont(new Font(BASE_FONT, Font.BOLD, 26)); setForeground(Color.WHITE);
              addMouseListener(new java.awt.event.MouseAdapter() {
                  @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                  @Override public void mouseExited (java.awt.event.MouseEvent e) { hover = false; repaint(); }
              }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(0,0,0,50)); g2.fillOval(4, 4, w-8, h-8);
                GradientPaint gp = new GradientPaint(0, 0, hover ? new Color(255,120,150) : GRAD_START,
                                                     w, h, hover ? new Color(150,120,255) : GRAD_END);
                g2.setPaint(gp); g2.fillOval(0, 0, w-8, h-8);
                super.paintComponent(g2); g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        run.setContentAreaFilled(false);
        run.setFocusPainted(false);
        run.setBorderPainted(false);
        run.setCursor(new Cursor(Cursor.HAND_CURSOR));

        f.setLayout(new BorderLayout()); f.add(run);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        f.setLocation(screen.width - 80, screen.height - 110);

        final Point[] click = new Point[1];
        run.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { click[0] = e.getPoint(); }
        });
        run.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseDragged(java.awt.event.MouseEvent e) {
                if (click[0] == null) return;
                Point p = f.getLocation();
                f.setLocation(p.x + e.getX() - click[0].x, p.y + e.getY() - click[0].y);
            }
        });

        run.addActionListener(e -> {
            long now = System.currentTimeMillis();
            if (sessionOpen && lastSessionStartMs > 0 && (now - lastSessionStartMs) > AUTO_RESET_MS) {
                try { if (currentSession != null) currentSession.close(); } catch (Exception ignored) {}
                sessionOpen = false; currentSession = null; activeDialog = null; interactionOpen = false;
            }

            if (sessionOpen && currentSession != null) {
                SwingUtilities.invokeLater(() -> {
                    boolean vis = currentSession.dialog.isVisible();
                    currentSession.dialog.setVisible(!vis);
                    if (!vis) { currentSession.dialog.toFront(); currentSession.dialog.requestFocus(); }
                });
                return;
            }
            if (interactionOpen && activeDialog != null) {
                SwingUtilities.invokeLater(() -> {
                    boolean vis = activeDialog.isVisible();
                    activeDialog.setVisible(!vis);
                    if (!vis) { activeDialog.toFront(); activeDialog.requestFocus(); }
                });
                return;
            }

            new Thread(() -> {
                try {
                    UIManager.put("Label.font", FONT_REGULAR);
                    UIManager.put("Button.font", FONT_REGULAR);
                    UIManager.put("TextArea.font", FONT_LARGE);
                    UIManager.put("OptionPane.background", Color.WHITE);
                    UIManager.put("Panel.background", Color.WHITE);

                    final String selectedCollection = getCollectionChoice();
                    if (selectedCollection == null || selectedCollection.isEmpty()) return;

                    final PromptSession session = buildChatWindow(selectedCollection);
                    currentSession = session; sessionOpen = true;
                    activeDialog = session.dialog; interactionOpen = true;
                    lastSessionStartMs = System.currentTimeMillis();

                    new Thread(() -> {
                        try {
                            long start = lastSessionStartMs;
                            Thread.sleep(AUTO_CLOSE_MS);
                            if (sessionOpen && currentSession == session && lastSessionStartMs == start) {
                                SwingUtilities.invokeLater(() -> {
                                    try { session.closeBtn.doClick(); } catch (Exception ignored) {}
                                });
                            }
                        } catch (InterruptedException ignored) {}
                    }, "auto-close-watchdog").start();

                } catch (HeadlessException ex) {
                    showError(ex.getMessage());
                    sessionOpen = false; currentSession = null; activeDialog = null; interactionOpen = false;
                } catch (Exception ex) {
                    showError("Error: " + ex.getMessage());
                    sessionOpen = false; currentSession = null; activeDialog = null; interactionOpen = false;
                }
            }, "controller-thread").start();
        });

        f.setVisible(true);
    }

    // ===== Build chat window (multi-page) =====
    public static PromptSession buildChatWindow(String selectedCollection) {
        final JButton backBtn = ghostButton("Change Assistant / Document Collection");
        backBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));

        final JButton closeBtn = ghostButtonArc("Close", 22);
        closeBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));

        final JButton copyBtn = primaryButton("Copy");
        copyBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        final JButton newChatBtn = primaryButton("New Chat");
        newChatBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        final JButton prevBtn = ghostButton("Prev");
        prevBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        final JButton nextBtn = ghostButton("Next");
        nextBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));

        final JDialog dialog = new JDialog((Frame) null, "New Chat Prompt", false);
        dialog.setSize(760, 720);
        dialog.setResizable(true);
        positionDialogAboveFloating(dialog);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(BG_WHITE);

        ShadowPanel card = new ShadowPanel(new BorderLayout(), RADIUS);

        // Header
        JPanel header = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRAD_START, getWidth(), getHeight(), GRAD_END);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 6, 18, 18);
                g2.dispose(); super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 68));
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(12, 18, 8, 18));

        JLabel title = new JLabel("Ask ICA");
        title.setFont(FONT_BOLD.deriveFont(16f));
        title.setForeground(Color.WHITE);

        JLabel collectionLabel = new JLabel(selectedCollection);
        collectionLabel.setFont(FONT_HINT);
        collectionLabel.setForeground(new Color(255, 245, 250, 200));

        JPanel headerLeft = new JPanel(new BorderLayout());
        headerLeft.setOpaque(false);
        headerLeft.add(title, BorderLayout.NORTH);
        headerLeft.add(collectionLabel, BorderLayout.SOUTH);
        header.add(headerLeft, BorderLayout.WEST);

        // Header Right: New Chat + Close (New Chat starts hidden and unlocks after first prompt)
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        headerRight.setOpaque(false);
        newChatBtn.setPreferredSize(new Dimension(96, 28));
        newChatBtn.setVisible(false);
        headerRight.add(newChatBtn);
        closeBtn.setPreferredSize(new Dimension(70, 28));
        headerRight.add(closeBtn);
        header.add(headerRight, BorderLayout.EAST);

        // Messages area
        JPanel messagesWrap = new JPanel(new BorderLayout());
        messagesWrap.setOpaque(false);
        messagesWrap.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel messages = new JPanel();
        messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
        messages.setOpaque(false);

        final JScrollPane scroll = new JScrollPane(messages,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        applyModernScroll(scroll);
        messagesWrap.add(scroll, BorderLayout.CENTER);

        // Input bar
        JPanel inputBar = new JPanel(new BorderLayout(8, 0));
        inputBar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        inputBar.setOpaque(false);

        final HintTextArea input = new HintTextArea();
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setRows(2);
        input.setFont(FONT_LARGE.deriveFont(14f));
        input.setBackground(new Color(245, 246, 248));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        input.setOpaque(true);
        input.setHint("Type your prompt here…");

        final JButton send = roundedButton("Send", GRAD_END, Color.WHITE);
        send.setFont(new Font(BASE_FONT, Font.BOLD, 14));
        send.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Actions row (left: prev/next, right: copy + back/change)
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(prevBtn);
        leftGroup.add(nextBtn);

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightGroup.setOpaque(false);
        copyBtn.setVisible(false);
        copyBtn.setEnabled(false);
        rightGroup.add(copyBtn);
        backBtn.setToolTipText("Change Assistant / Document Collection (before first prompt)");
        rightGroup.add(backBtn);

        actions.add(leftGroup, BorderLayout.WEST);
        actions.add(rightGroup, BorderLayout.EAST);

        // Typing indicator host
        JPanel typingHost = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        typingHost.setOpaque(false);
        typingHost.setBorder(BorderFactory.createEmptyBorder(6, 16, 0, 16));

        // Copy acts on the last assistant message of current page
        copyBtn.addActionListener(e -> {
            copyBtn.setEnabled(false);
            Timer t = new Timer(400, evt -> {
                try {
                    Conversation c = currentSession != null ? currentSession.currentConversation() : null;
                    String txt = (c != null) ? c.lastAssistant : "";
                    setClipboardText(txt == null ? "" : txt);
                } finally {
                    copyBtn.setEnabled(true);
                    if (currentSession != null) currentSession.updateHeaderForCurrent();
                }
            });
            t.setRepeats(false);
            t.start();
        });

        // Back: selection allowed only before first user message
        backBtn.addActionListener(e -> {
            if (currentSession == null) { dialog.setVisible(false); return; }
            Conversation conv = currentSession.currentConversation();
            if (conv == null) { dialog.setVisible(false); return; }

            boolean anyUserMessage = conv.hasAnyUserMessage();
            if (!anyUserMessage) {
                String newSel = getCollectionChoice();
                if (newSel == null || newSel.isEmpty()) return;

                Conversation replacement = new Conversation(newSel, conv.title);
                replacement.prepared = false;

                currentSession.conversations.set(currentSession.currentConversationIndex, replacement);
                currentSession.renderConversation(currentSession.currentConversationIndex);
                return;
            }
            dialog.setVisible(false);
        });

        closeBtn.addActionListener(e -> {
            if (currentSession != null) currentSession.close();
            else {
                SwingUtilities.invokeLater(dialog::dispose);
                sessionOpen = false; currentSession = null; activeDialog = null; interactionOpen = false;
            }
        });

        // Input scroll (cap height)
        final JScrollPane inputScroll = new JScrollPane(input,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setBorder(input.getBorder());
        input.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        applyModernScroll(inputScroll);
        final int INPUT_H = 74;
        inputScroll.setPreferredSize(new Dimension(0, INPUT_H));
        inputScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, INPUT_H));

        inputBar.add(inputScroll, BorderLayout.CENTER);
        final int SEND_W = 88, SEND_H = 36;
        send.setPreferredSize(new Dimension(SEND_W, SEND_H));
        JPanel sendWrap = new JPanel(new GridBagLayout());
        sendWrap.setOpaque(false); sendWrap.add(send);
        inputBar.add(sendWrap, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);
        card.add(messagesWrap, BorderLayout.CENTER);
        JPanel southWrap = new JPanel(new BorderLayout());
        southWrap.setOpaque(false);
        southWrap.add(actions, BorderLayout.NORTH);
        southWrap.add(typingHost, BorderLayout.CENTER);
        southWrap.add(inputBar, BorderLayout.SOUTH);
        card.add(southWrap, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH; gbc.insets = new Insets(6, 6, 6, 6);
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        dialog.add(card, gbc);

        dialog.setUndecorated(true);
        makeWindowRounded(dialog, 18);
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 14));
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                activeDialog = null; interactionOpen = false;
            }
        });
        dialog.getRootPane().registerKeyboardAction(
                e -> { dialog.dispose(); activeDialog = null; interactionOpen = false; },
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        final PromptSession session = new PromptSession(dialog, backBtn, copyBtn, closeBtn);
        session.newChatBtn = newChatBtn; session.prevBtn = prevBtn; session.nextBtn = nextBtn;
        session.sendBtn = send; session.inputArea = input; session.messagesPanel = messages; session.messagesScroll = scroll;
        session.headerCollectionLabel = collectionLabel;
        session.typingHost = typingHost;

        session.inputBarPanel      = inputBar;
        session.actionsLeftGroup   = leftGroup;
        session.actionsRightGroup  = rightGroup;
        session.actionsRow         = actions;
        session.inputScrollPane    = inputScroll;

        Conversation first = new Conversation(selectedCollection, "Chat 1");
        session.conversations.add(first);
        session.currentConversationIndex = 0;

        session.setSendControls(
            () -> { send.setEnabled(false); input.setEnabled(false); },
            () -> {
                boolean isLatest = session.currentConversationIndex == session.conversations.size() - 1;
                send.setEnabled(isLatest); input.setEnabled(isLatest);
            }
        );

        final Runnable doSend = () -> {
            String txt = input.getText().trim();
            if (txt.isEmpty()) { Toolkit.getDefaultToolkit().beep(); return; }
            session.addMessage(txt, true);
            input.setText("");

            // Unlock header New Chat after the first prompt in the first chat
            if (!session.newChatUnlocked && session.currentConversationIndex == 0) {
                session.newChatUnlocked = true;
                SwingUtilities.invokeLater(session::updateHeaderForCurrent);
            }

            Conversation conv = session.currentConversation();
            if (conv == null) return;

            session.setSending(true);
            new Thread(() -> {
                String answer;
                try {
                    if (!conv.prepared) {
                        SharedBrowser.startNewChat(conv.selectedCollection);
                        conv.prepared = true;
                    }
                    WebDriver d = SharedBrowser.getDriver();
                    WebElement chatInput = locateChatInput(d);
                    ((JavascriptExecutor) d).executeScript("arguments[0].focus();", chatInput);

                    String token = "seen-" + System.currentTimeMillis();
                    markExistingAssistantBlocks(d, token);

                    try { chatInput.clear(); } catch (Exception ignored) {}
                    try { chatInput.sendKeys(txt); } catch (Exception ignored) {}
                    setTextAndDispatch(d, chatInput, txt);

                    boolean sent = clickSendButtonScoped(d, chatInput, 6000);
                    if (!sent) { try { chatInput.sendKeys(Keys.ENTER); sent = true; } catch (Exception ignored) {} }
                    if (!sent) { try { chatInput.sendKeys(Keys.chord(Keys.CONTROL, Keys.ENTER)); sent = true; } catch (Exception ignored) {} }
                    if (!sent) {
                        try {
                            ((JavascriptExecutor) d).executeScript(
                                "['keydown','keypress','keyup'].forEach(t=>{" +
                                "  const e=new KeyboardEvent(t,{key:'Enter',code:'Enter',which:13,keyCode:13,bubbles:true});" +
                                "  arguments[0].dispatchEvent(e);" +
                                "});", chatInput
                            );
                        } catch (Exception ignored) {}
                    }

                    WebElement lastAnswer = waitForNewAssistantBlock(d, token, 90_000);
                    if (lastAnswer == null) { answer = ""; }
                    else {
                        ((JavascriptExecutor) d).executeScript("arguments[0].scrollIntoView({block:'center'})", lastAnswer);
                        waitForNonEmptyContent(d, lastAnswer, 45);
                        answer = tryExtractAnswer(d, lastAnswer);
                        if (answer == null || answer.trim().isEmpty()) {
                            waitForContentToStabilize(d, lastAnswer, 4000);
                            answer = smartExtractCleanText(d, lastAnswer);
                        }
                    }
                    if (answer == null || answer.trim().isEmpty()) answer = "(no response)";
                } catch (InterruptedException ex2) {
                    answer = "(interrupted)";
                } catch (Exception ex1) {
                    answer = "(error: " + ex1.getMessage() + ")";
                }
                session.onAssistantResponse(answer);
                session.setSending(false);
                session.updateHeaderForCurrent();
            }, "send-thread").start();
        };

        send.addActionListener(e -> doSend.run());

        // Key bindings: Enter = Send, Shift+Enter = newline
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "SEND_CHAT");
        input.getActionMap().put("SEND_CHAT", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { doSend.run(); }});
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "INSERT_BREAK");
        input.getActionMap().put("INSERT_BREAK", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { input.append("\n"); }});
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { input.repaint(); }
            @Override public void removeUpdate(DocumentEvent e) { input.repaint(); }
            @Override public void changedUpdate(DocumentEvent e) { input.repaint(); }
        });
        input.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { input.repaint(); }
            @Override public void focusLost(FocusEvent e) { input.repaint(); }
        });

        prevBtn.addActionListener(e -> {
            int idx = Math.max(0, session.currentConversationIndex - 1);
            session.renderConversation(idx);
        });
        nextBtn.addActionListener(e -> {
            int idx = Math.min(session.conversations.size() - 1, session.currentConversationIndex + 1);
            session.renderConversation(idx);
        });

        newChatBtn.addActionListener(e -> {
            String sel = getCollectionChoice();
            if (sel == null || sel.isEmpty()) return;

            int newIndex = session.conversations.size();
            Conversation conv = new Conversation(sel, "Chat " + (newIndex + 1));
            session.conversations.add(conv);
            session.renderConversation(newIndex);
        });

        prevBtn.setVisible(false);
        nextBtn.setVisible(false);
        if (typingHost != null) typingHost.setVisible(true);
        if (inputBar != null) inputBar.setVisible(true);

        session.updateHeaderForCurrent();

        dialog.setVisible(true);
        return session;
    }

    // ===== Selection dialog (Assistant or Document Collection) =====
    public static String getCollectionChoice() {
    final JDialog dialog = new JDialog((Frame) null, "Choose Source Type", true);
    dialog.setSize(520, 430); // clean compact professional size
    dialog.setMinimumSize(new Dimension(520, 430));
    positionDialogAboveFloating(dialog);
    dialog.setLayout(new GridBagLayout());
    dialog.getContentPane().setBackground(new Color(0,0,0,0));

    ShadowPanel card = new ShadowPanel(new BorderLayout(), RADIUS);

    // ===== Header (gradient) =====
    JPanel header = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, GRAD_START, getWidth(), getHeight(), GRAD_END);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight() + 6, 18, 18);
            g2.dispose(); super.paintComponent(g);
        }
    };
    header.setOpaque(false);
    header.setPreferredSize(new Dimension(0, 82));
    header.setLayout(new BorderLayout());
    header.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));

    JLabel title = new JLabel("Select Input Source & Sign In");
    title.setFont(FONT_BOLD.deriveFont(17f));
    title.setForeground(Color.WHITE);

    JPanel headerLeft = new JPanel(new BorderLayout());
    headerLeft.setOpaque(false);
    headerLeft.add(title, BorderLayout.NORTH);
    header.add(headerLeft, BorderLayout.WEST);

    // ===== Body =====
    JPanel body = new JPanel();
    body.setOpaque(false);
    body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
    body.setBorder(BorderFactory.createEmptyBorder(16, 26, 10, 26));

    // --- Email controls (NEW) ---
    JLabel emailLabel = new JLabel("Login ID");
    emailLabel.setFont(FONT_SEMIBOLD.deriveFont(15f));
    emailLabel.setForeground(TEXT);
    emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

    javax.swing.JTextField emailField = new javax.swing.JTextField();
    emailField.setFont(FONT_LARGE.deriveFont(15f));
    emailField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
    ));
    emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
    emailField.setAlignmentX(Component.LEFT_ALIGNMENT);

    
String saved = PREFS.get(PREF_KEY_USER_EMAIL, "");
if (USER_EMAIL != null && !USER_EMAIL.isBlank()) saved = USER_EMAIL;
emailField.setText(saved);



JPanel dividerTop = new JPanel();
dividerTop.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
dividerTop.setPreferredSize(new Dimension(1, 1));
dividerTop.setBackground(new Color(220, 223, 230));
dividerTop.setAlignmentX(Component.LEFT_ALIGNMENT);

    // --- Source type radios + dropdown (existing UI) ---//
    JPanel radiosWrap = new JPanel(new FlowLayout(FlowLayout.LEFT,20, 4));
    radiosWrap.setOpaque(false);
    radiosWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

    final JRadioButton docRadio = new JRadioButton("Document Collection");
    final JRadioButton agentRadio = new JRadioButton("Assistant & Agent");
    docRadio.setFont(FONT_REGULAR.deriveFont(14.5f));
    agentRadio.setFont(FONT_REGULAR.deriveFont(14.5f));
    docRadio.setForeground(TEXT);
    agentRadio.setForeground(TEXT);
    docRadio.setOpaque(false);
    agentRadio.setOpaque(false);
    docRadio.setSelected(true);

    ButtonGroup group = new ButtonGroup();
    group.add(docRadio); group.add(agentRadio);
    radiosWrap.add(docRadio); radiosWrap.add(agentRadio);

    JPanel divider = new JPanel();
    divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
    divider.setPreferredSize(new Dimension(1, 1));
    divider.setBackground(new Color(220, 223, 230));

    JLabel chooseLabel = new JLabel("Document Collection");
    chooseLabel.setFont(FONT_SEMIBOLD.deriveFont(14.5f));
    chooseLabel.setForeground(MUTED);
    chooseLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

    String[] docOptions = {
        "— Select a collection —",
        "Centre-Led EDICOM",
        "SRM BaU DocuSphere",
        "Zycus Eproc Docusphere",
        "Zycus StC Docusphere"
    };
    String[] agentOptions = {
        "— Select an assistant —",
        "Email Generator",
        "Effective Communicator",
        "Internet Fact Checker",
        "Document Generator",
        "SAP Astral",
        "SDLC Advisor Assistant - Design",
        "SDLC Advisor Assistant - Discovery",
        "SDLC Advisor Assistant - Build & Test",
        "SDLC Advisor Assistant - Deploy & Manage",
        "Generate User Story",
        "Notes Analyzer",
        "Chart Creator",
        "Convert GraphML to JSON",
        "Convert JSON to JOLT",
        "Convert YAML to JSON",
        "Create sample data for XML/JSON Schema",
        "Detect Language",
        "Diagram Generator",
        "Generate Test Data",
        "Generate User Stories from Epic",
        "Image Generator",
        "Internet Researcher",
        "Code Documentor",
        "SAPer",
        "Test Script Generator"
    };

    final JComboBox<String> combo = new JComboBox<>(docOptions);
    combo.setFont(FONT_REGULAR.deriveFont(14.5f));
    combo.setPreferredSize(new Dimension(460, 70));
    combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
    combo.setBackground(CARD);
    combo.setRenderer(new PlaceholderComboRenderer(0));
    combo.setSelectedIndex(0);
    combo.setBorder(
        BorderFactory.createCompoundBorder(
            roundedBorder(BORDER),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        )
    );
    combo.setAlignmentX(Component.LEFT_ALIGNMENT);
    combo.setRenderer(new PlaceholderComboRenderer(0));

    // Apply modern C1 UI (rounded, arrow)
    combo.setUI(new ModernComboBoxUI());
    addFocusBorder(combo);

    docRadio.addActionListener(e -> {
        combo.removeAllItems();
        for (String s : docOptions) combo.addItem(s);
        combo.setRenderer(new PlaceholderComboRenderer(0));
        combo.setSelectedIndex(0);
        chooseLabel.setText("Document Collection");
    });
    agentRadio.addActionListener(e -> {
        combo.removeAllItems();
        for (String s : agentOptions) combo.addItem(s);
        combo.setRenderer(new PlaceholderComboRenderer(0));
        combo.setSelectedIndex(0);
        chooseLabel.setText("Assistant & Agent");
    });

    // Assemble body
    body.add(emailLabel);
    body.add(Box.createVerticalStrut(6));
    body.add(emailField);
    body.add(Box.createVerticalStrut(16));
    body.add(radiosWrap);
    body.add(Box.createVerticalStrut(16));
    body.add(chooseLabel);
    body.add(Box.createVerticalStrut(6));
    body.add(combo);

    // ===== Footer =====
    JPanel footerTint = new JPanel(new BorderLayout());
    footerTint.setOpaque(true);
    footerTint.setBackground(new Color(248, 249, 252));
    footerTint.setBorder(BorderFactory.createEmptyBorder(10, 18, 12, 18));

    JButton ok = primaryButton("OK");
    JButton cancel = ghostButton("Cancel");
    ok.setPreferredSize(new Dimension(90, 34));
    cancel.setPreferredSize(new Dimension(90, 34));

    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
    footer.setOpaque(false);
    footer.add(cancel);
    footer.add(ok);
    footerTint.add(footer, BorderLayout.EAST);

    // Put parts into card
    card.add(header, BorderLayout.NORTH);
    card.add(body, BorderLayout.CENTER);
    card.add(footerTint, BorderLayout.SOUTH);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH; gbc.insets = new Insets(6, 6, 6, 6);
    gbc.weightx = 1.0; gbc.weighty = 1.0;
    dialog.add(card, gbc);

    dialog.setUndecorated(true);
    makeWindowRounded(dialog, 28);
    dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(6, 8, 10, 8));

    final String[] result = new String[1];

    Runnable confirm = () -> {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        // Basic client-side validation
        if (email.isEmpty() || !email.contains("@") || email.startsWith("@") || email.endsWith("@") || email.endsWith(".")) {
            JOptionPane.showMessageDialog(dialog,
                    "Please enter a valid IBM email address (e.g., name@ibm.com).",
                    "Invalid email", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (combo.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(dialog, "Please select a valid option.", "Select", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Persist + store in session
        USER_EMAIL = email;
        
        String mode = docRadio.isSelected() ? "DOC" : "AGENT";
        result[0] = mode + "::" + combo.getSelectedItem();
        dialog.dispose();
    };

    ok.addActionListener(e -> confirm.run());
    cancel.addActionListener(e -> { result[0] = null; dialog.dispose(); });

    dialog.getRootPane().setDefaultButton(ok);
    dialog.getRootPane().registerKeyboardAction(
            e -> { result[0] = null; dialog.dispose(); },
            KeyStroke.getKeyStroke("ESCAPE"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
    );

    dialog.setVisible(true);
    return result[0];
}

    // ================= MAIN =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createFloatingController);
    }

    // ====== EXTRA HELPERS ======
    private static void waitForContentToStabilize(WebDriver driver, WebElement element, int maxMillis) {
        String previousText = "";
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < maxMillis) {
            try {
                String currentText = element.getText();
                if (currentText != null && currentText.equals(previousText)) return;
                previousText = currentText;
                Thread.sleep(300);
            } catch (StaleElementReferenceException | InterruptedException ignored) { }
        }
    }
    private static void waitForNonEmptyContent(WebDriver driver, WebElement container, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.until(d -> {
            try {
                String text = container.getText();
                return text != null && !text.trim().isEmpty();
            } catch (StaleElementReferenceException e) { return false; }
        });
    }
}
