// CBtn.java - 커스텀 버튼 컴포넌트
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class CBtn extends JPanel {
    private String text;
    private Color bgOn, bgOff, bgDisabled;
    private boolean enabled = true;   // 버튼 활성/비활성
    private boolean hovered = false;  // 마우스 호버 상태
    private boolean toggle = false;   // 토글 버튼 여부
    private boolean selected = false; // 토글 ON/OFF 상태
    private java.util.List<ActionListener> listeners = new ArrayList<>();

    // 일반 버튼 생성자 (토글 아님, 색상 1개만 사용)
    public CBtn(String text, Color bg) {
        this(text, bg, bg, false);
    }

    // 토글 버튼 생성자 (ON일 때 색상, OFF일 때 색상 구분)
    public CBtn(String text, Color bgOn, Color bgOff, boolean isToggle) {
        this.text = (text == null) ? "" : text;
        this.bgOn = bgOn;
        this.bgOff = bgOff;
        this.bgDisabled = new Color(180, 180, 180);
        this.toggle = isToggle;
        setOpaque(false);
        setPreferredSize(new Dimension(100, 36));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (enabled) { hovered = true; repaint(); }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false; repaint();
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!enabled) return; // 비활성 상태면 클릭 무시
                if (toggle) selected = !selected;
                repaint();
                // 등록된 모든 리스너에게 클릭 이벤트 전달
                ActionEvent ae = new ActionEvent(CBtn.this, ActionEvent.ACTION_PERFORMED, text);
                for (ActionListener l : listeners) {
                    try {
                        l.actionPerformed(ae);
                    } catch (Exception ex) {
                        // 리스너 내부에서 예외가 나도 버튼 자체는 죽지 않도록 보호
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    public void setText(String t) { this.text = (t == null) ? "" : t; repaint(); }
    public String getText() { return text; }
    public void addActionListener(ActionListener l) {
        if (l != null) listeners.add(l);
    }
    public void setEnabled(boolean b) { this.enabled = b; repaint(); }
    public boolean isEnabled() { return enabled; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean s) { this.selected = s; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 상태에 따른 배경색 결정
            Color bg;
            if (!enabled) {
                bg = bgDisabled;
            } else if (toggle && selected) {
                bg = hovered ? bgOn.brighter() : bgOn;
            } else if (!toggle) {
                bg = hovered ? bgOn.brighter() : bgOn;
            } else {
                bg = hovered ? bgOff.brighter() : bgOff;
            }

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

            // 버튼 글씨 (항상 흰색, 가운데 정렬)
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(text)) / 2;
            int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(text, tx, ty);
        } finally {
            g2.dispose(); // Graphics 리소스는 항상 해제
        }
    }
}
