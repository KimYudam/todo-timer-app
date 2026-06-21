// FocusOverlay.java - 집중 모드 오버레이 창
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class FocusOverlay extends JDialog {
    private JLabel taskLabel, timeLabel, modeLabel, distractLabel;
    private int distractCount = 0;       // 집중 이탈(다른 창 전환) 횟수
    private Runnable stopCallback;       // 정지 버튼/ESC 클릭 시 실행할 동작

    public FocusOverlay(JFrame owner) {
        super(owner, true); // modal = true : 메인 창 조작 차단
        setUndecorated(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // X 버튼으로 못 닫게
        setSize(700, 680);

        // 어두운 전체 배경 패널
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(18, 22, 42));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setOpaque(true);

        // 중앙 정보 영역 (모드명/작업명/타이머/이탈횟수)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(80, 40, 0, 40));

        modeLabel = makeLabel("집중 모드", new Font("맑은 고딕", Font.BOLD, 32), new Color(80, 210, 255));
        taskLabel = makeLabel("작업명",   new Font("맑은 고딕", Font.BOLD, 20), new Color(200, 200, 200));
        timeLabel = makeLabel("00:00:00", new Font("Courier New", Font.BOLD, 70), Color.WHITE);
        distractLabel = makeLabel("집중 이탈: 0회", new Font("맑은 고딕", Font.PLAIN, 16), new Color(255, 180, 100));

        centerPanel.add(modeLabel);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(taskLabel);
        centerPanel.add(Box.createVerticalStrut(30));
        centerPanel.add(timeLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(distractLabel);

        // 하단 정지 버튼 영역
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 200, 60, 200));

        CBtn stopBtn = new CBtn("정지  (ESC)", new Color(210, 50, 50));
        stopBtn.setPreferredSize(new Dimension(260, 52));
        stopBtn.setMaximumSize(new Dimension(260, 52));
        stopBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopBtn.addActionListener(e -> safeStop());

        JLabel hint = makeLabel("클릭 또는 ESC 키로 정지", new Font("맑은 고딕", Font.PLAIN, 13), new Color(130, 130, 160));

        bottomPanel.add(stopBtn);
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(hint);

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(panel);

        // ESC 키로도 정지 가능하게 등록
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "stop");
        panel.getActionMap().put("stop", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                safeStop();
            }
        });
    }

    // stopCallback이 비어있거나 내부에서 예외가 나도 오버레이가 멈추지 않도록 보호
    private void safeStop() {
        if (stopCallback == null) return;
        try {
            stopCallback.run();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private JLabel makeLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    public void setStopCallback(Runnable cb) { stopCallback = cb; }
    public void setTaskName(String name) { taskLabel.setText("[ " + (name == null ? "" : name) + " ]"); }
    public void setModeLabel(String mode) { modeLabel.setText(mode); }
    public void updateTime(String time)   { timeLabel.setText(time); }

    // 집중 이탈(다른 창 전환) 1회 발생 시 호출
    public void incrementDistract() {
        distractCount++;
        distractLabel.setText("집중 이탈: " + distractCount + "회  !");
        distractLabel.setForeground(distractCount > 3 ? new Color(255, 70, 70) : new Color(255, 180, 100));
    }

    public int getDistractCount() { return distractCount; }
    public void resetDistractCount() { distractCount = 0; }
}
