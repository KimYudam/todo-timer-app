// TodoTimerApp.java - 메인 애플리케이션 (전체 UI + 로직 총괄)
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.io.*;
import java.time.LocalDate;

public class TodoTimerApp extends JFrame {

    // 데이터
    private ArrayList<TodoItem> todoList = new ArrayList<>();
    private int selectedIndex = -1; // 현재 선택된 할 일의 인덱스 (-1 = 선택 없음)

    // 타이머 상태
    private Timer swingTimer;          // 1초마다 동작하는 Swing Timer
    private long sessionSeconds = 0;   // 이번 실행에서 흐른 시간(초)
    private boolean isRunning = false; // 타이머 실행 여부

    // 뽀모도로 설정
    private int focusMinutes = 25;            // 집중 시간(분)
    private int breakMinutes = 5;             // 휴식 시간(분)
    private boolean isPomodoroMode = false;   // 뽀모도로 모드 ON/OFF
    private boolean isBreakTime = false;      // 현재 휴식 단계인지 여부
    private long pomodoroTargetSeconds = 0;   // 현 단계의 목표 시간(초)
    private int pomodoroRound = 0;            // 현재 라운드 번호

    // 집중 모드
    private boolean focusModeEnabled = false; // 집중 모드 ON/OFF
    private FocusOverlay focusOverlay;        // 집중 모드 오버레이 창

    // UI 컴포넌트
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> todoJList;
    private JTextField inputField;
    private JLabel timerLabel, statusLabel, pomodoroInfoLabel, roundLabel;
    private CBtn startBtn, stopBtn, resetBtn, completeBtn;
    private CBtn focusModeToggle, pomodoroToggle;
    private JComboBox<String> focusTimeCombo, breakTimeCombo;
    private JProgressBar pomodoroProgress;

    public TodoTimerApp() {
        setTitle("Todo 타이머 - 집중 관리");
        // 종료 동작을 직접 제어 (닫기 전에 자동 저장을 하기 위해 DO_NOTHING 사용)
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(700, 680);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
        focusOverlay = new FocusOverlay(this);
        focusOverlay.setStopCallback(() -> SwingUtilities.invokeLater(this::stopTimer));
        initTimer();
        initFocusListener();

        // 앱 시작 시 저장된 데이터 불러오기 (파일이 없거나 손상되어도 앱은 정상 시작됨)
        loadData();

        // 창 닫기(X 버튼) 시 자동 저장 후 종료
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();
                System.exit(0);
            }
        });

        setVisible(true);
    }

    //  UI 초기화
    private void initUI() {
        getContentPane().setBackground(new Color(240, 243, 250));
        setLayout(new BorderLayout(10, 10));
        add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(0, 15, 0, 15));
        center.add(createTodoPanel(),  BorderLayout.CENTER);
        center.add(createTimerPanel(), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    // 상단 헤더 (제목 + 상태 메시지)
    private JPanel createHeaderPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(45, 58, 95));
        p.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Todo 타이머");
        title.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        statusLabel = new JLabel("할 일을 선택하고 타이머를 시작하세요");
        statusLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(190, 210, 245));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        p.add(title,       BorderLayout.WEST);
        p.add(statusLabel, BorderLayout.EAST);
        return p;
    }

    // 할 일 목록 영역 (입력창 + 추가/삭제 버튼 + 리스트)
    private JPanel createTodoPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 8));
        panel.setOpaque(false);
        panel.setBorder(titledBorder("  할 일 목록  "));

        JPanel inputRow = new JPanel(new BorderLayout(5, 0));
        inputRow.setOpaque(false);
        inputRow.setBorder(new EmptyBorder(5, 5, 5, 5));

        inputField = new JTextField();
        inputField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        inputField.setForeground(Color.BLACK);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(170, 185, 210), 1, true),
            new EmptyBorder(6, 10, 6, 10)));
        inputField.addActionListener(e -> addTodo()); // Enter 키로도 추가 가능

        CBtn addBtn = new CBtn("+ 추가", new Color(50, 115, 185));
        addBtn.setPreferredSize(new Dimension(75, 36));
        addBtn.setMaximumSize(new Dimension(75, 36));
        addBtn.addActionListener(e -> addTodo());

        CBtn delBtn = new CBtn("삭제", new Color(185, 60, 60));
        delBtn.setPreferredSize(new Dimension(60, 36));
        delBtn.setMaximumSize(new Dimension(60, 36));
        delBtn.addActionListener(e -> deleteTodo());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btnRow.setOpaque(false);
        btnRow.add(addBtn);
        btnRow.add(delBtn);

        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(btnRow,     BorderLayout.EAST);

        todoJList = new JList<>(listModel);
        todoJList.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        todoJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        todoJList.setFixedCellHeight(38);
        todoJList.setBackground(Color.WHITE);
        todoJList.setForeground(Color.BLACK);
        todoJList.setBorder(new EmptyBorder(4, 8, 4, 8));
        todoJList.setCellRenderer(new TodoCellRenderer());
        todoJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedIndex = todoJList.getSelectedIndex();
                updateStatus();
            }
        });

        JScrollPane scroll = new JScrollPane(todoJList);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 208, 220), 1));
        scroll.setPreferredSize(new Dimension(370, 280));

        panel.add(inputRow, BorderLayout.NORTH);
        panel.add(scroll,   BorderLayout.CENTER);
        return panel;
    }

    // 타이머 + 뽀모도로 + 집중 모드 + 제어 버튼이 모인 우측 패널
    private JPanel createTimerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(242, 0));
        panel.setOpaque(false);
        panel.setBorder(titledBorder("  타이머 & 설정  "));

        timerLabel = new JLabel("00:00:00");
        timerLabel.setFont(new Font("Courier New", Font.BOLD, 36));
        timerLabel.setForeground(new Color(30, 40, 80));
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        roundLabel = new JLabel(" ");
        roundLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        roundLabel.setForeground(new Color(180, 80, 20));
        roundLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        pomodoroInfoLabel = new JLabel(" ");
        pomodoroInfoLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        pomodoroInfoLabel.setForeground(new Color(60, 80, 120));
        pomodoroInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        pomodoroProgress = new JProgressBar(0, 100);
        pomodoroProgress.setValue(0);
        pomodoroProgress.setStringPainted(false);
        pomodoroProgress.setForeground(new Color(70, 160, 70));
        pomodoroProgress.setBackground(new Color(215, 225, 238));
        pomodoroProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        pomodoroProgress.setBorderPainted(false);

        // 뽀모도로 ON/OFF 토글 (ON=주황, OFF=회색)
        pomodoroToggle = new CBtn("뽀모도로 OFF",
            new Color(210, 95, 30), new Color(140, 145, 160), true);
        pomodoroToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
        pomodoroToggle.addActionListener(e -> togglePomodoro());

        // 집중/휴식 시간 선택 콤보박스 행
        JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        timeRow.setOpaque(false);
        timeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        timeRow.add(blackLabel("집중:"));
        String[] ft = {"10분","15분","20분","25분","30분","45분","60분","90분"};
        focusTimeCombo = combo(ft, "25분");
        timeRow.add(focusTimeCombo);
        timeRow.add(blackLabel("휴식:"));
        String[] bt = {"3분","5분","10분","15분"};
        breakTimeCombo = combo(bt, "5분");
        timeRow.add(breakTimeCombo);

        // 집중 모드 ON/OFF 토글 (ON=빨강, OFF=회색)
        focusModeToggle = new CBtn("집중 모드 OFF",
            new Color(185, 45, 45), new Color(140, 145, 160), true);
        focusModeToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
        focusModeToggle.addActionListener(e -> toggleFocusMode());

        startBtn    = new CBtn("시작",   new Color(45, 150, 75));
        stopBtn     = new CBtn("정지",   new Color(200, 85, 45));
        resetBtn    = new CBtn("초기화", new Color(105, 110, 150));
        completeBtn = new CBtn("완료",   new Color(65, 110, 190));

        for (CBtn b : new CBtn[]{startBtn, stopBtn, resetBtn, completeBtn}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        startBtn.addActionListener(e -> startTimer());
        stopBtn.addActionListener(e -> stopTimer());
        resetBtn.addActionListener(e -> resetTimer());
        completeBtn.addActionListener(e -> toggleComplete());
        stopBtn.setEnabled(false); // 시작 전에는 정지 버튼 비활성

        panel.add(Box.createVerticalStrut(8));
        panel.add(timerLabel);
        panel.add(Box.createVerticalStrut(3));
        panel.add(roundLabel);
        panel.add(Box.createVerticalStrut(3));
        panel.add(pomodoroInfoLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(pomodoroProgress);
        panel.add(Box.createVerticalStrut(10));
        panel.add(pomodoroToggle);
        panel.add(Box.createVerticalStrut(5));
        panel.add(timeRow);
        panel.add(Box.createVerticalStrut(5));
        panel.add(focusModeToggle);
        panel.add(Box.createVerticalStrut(10));
        panel.add(startBtn);
        panel.add(Box.createVerticalStrut(4));
        panel.add(stopBtn);
        panel.add(Box.createVerticalStrut(4));
        panel.add(resetBtn);
        panel.add(Box.createVerticalStrut(4));
        panel.add(completeBtn);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    // 하단 안내 문구 패널
    private JPanel createBottomPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 7));
        p.setBackground(new Color(228, 233, 242));
        p.setBorder(new MatteBorder(1, 0, 0, 0, new Color(200, 208, 220)));
        JLabel info = new JLabel("집중 모드 ON → 타이머 시작 시 화면 잠금 활성화  |  정지: 오버레이 버튼 또는 ESC 키");
        info.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        info.setForeground(new Color(60, 75, 110));
        p.add(info);
        return p;
    }

    //  UI 생성 보조 메서드
    private TitledBorder titledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(185, 198, 220), 1, true),
            title, TitledBorder.LEFT, TitledBorder.TOP,
            new Font("맑은 고딕", Font.BOLD, 13), new Color(40, 58, 105));
    }

    private JLabel blackLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        l.setForeground(Color.BLACK);
        return l;
    }

    private JComboBox<String> combo(String[] items, String sel) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setSelectedItem(sel);
        c.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        c.setForeground(Color.BLACK);
        c.setPreferredSize(new Dimension(65, 28));
        c.addActionListener(e -> updatePomodoroSettings());
        return c;
    }

    //  타이머 핵심 로직 (1초마다 실행)
    private void initTimer() {
        swingTimer = new Timer(1000, e -> {
            sessionSeconds++;
            timerLabel.setText(fmt(sessionSeconds));

            // 선택된 할 일이 유효하고 휴식 중이 아니면 누적 시간 증가
            if (selectedIndex >= 0 && selectedIndex < todoList.size() && !isBreakTime) {
                todoList.get(selectedIndex).addSeconds(1);
                refreshList();
            }

            // 집중 모드 오버레이가 떠 있으면 시간 동기화
            if (focusModeEnabled && focusOverlay.isVisible()) {
                focusOverlay.updateTime(fmt(sessionSeconds));
            }

            // 뽀모도로 모드: 진행률 갱신 및 단계 종료 체크
            if (isPomodoroMode) {
                if (pomodoroTargetSeconds > 0) {
                    int pct = (int) Math.min(sessionSeconds * 100 / pomodoroTargetSeconds, 100);
                    pomodoroProgress.setValue(pct);
                }
                if (sessionSeconds >= pomodoroTargetSeconds) {
                    handlePomodoroPhaseEnd();
                }
            }
        });
    }

    // 경고음을 Java Sound API로 직접 생성해서 재생
    // (Toolkit.beep()은 Windows 설정에 따라 무음일 수 있어 신뢰할 수 없음)
    private void playBeep() {
        try {
            float sampleRate = 8000f;
            int duration = 300; // ms
            byte[] buf = new byte[(int) (sampleRate * duration / 1000)];
            for (int i = 0; i < buf.length; i++) {
                double angle = i / (sampleRate / 880.0) * 2.0 * Math.PI;
                buf[i] = (byte) (Math.sin(angle) * 80);
            }
            javax.sound.sampled.AudioFormat af =
                new javax.sound.sampled.AudioFormat(sampleRate, 8, 1, true, false);
            javax.sound.sampled.SourceDataLine sdl =
                javax.sound.sampled.AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();
            sdl.write(buf, 0, buf.length);
            sdl.drain();
            sdl.close();
        } catch (Exception ex) {
            // 사운드 장치를 못 열거나 오류가 나면 시스템 기본 비프음으로 대체
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Exception ignored) {
                // 비프음마저 실패하면 그냥 무시 (앱이 멈추지 않게)
            }
        }
    }

    // Windows 보안 정책으로 toFront()가 막히는 문제를 우회하기 위해
    // setAlwaysOnTop(true)를 일시적으로 적용해 강제로 창을 앞으로 가져옴
    private void forceToFront(Window w) {
        if (w == null) return;
        try {
            w.setAlwaysOnTop(true);
            w.toFront();
            w.requestFocus();
            // 오버레이가 아닌 경우에는 잠시 후 alwaysOnTop 해제 (계속 켜두면 불편함)
            if (w != focusOverlay) {
                Timer t = new Timer(200, e -> {
                    try { w.setAlwaysOnTop(false); } catch (Exception ignored) {}
                });
                t.setRepeats(false);
                t.start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // 메인 창 / 오버레이 창의 포커스 이탈을 감시하여 집중 모드를 보조
    private void initFocusListener() {
        // 메인 창에서 포커스가 빠져나가는 경우 감시
        addWindowFocusListener(new WindowFocusListener() {
            @Override public void windowGainedFocus(WindowEvent e) {}
            @Override
            public void windowLostFocus(WindowEvent e) {
                // 오버레이로 이동하는 정상적인 전환은 이탈로 보지 않음
                if (e.getOppositeWindow() == focusOverlay) return;
                if (isRunning && focusModeEnabled) {
                    focusOverlay.incrementDistract();
                    new Thread(() -> playBeep()).start();
                    SwingUtilities.invokeLater(() -> {
                        if (focusOverlay.isVisible()) {
                            forceToFront(focusOverlay);
                        } else {
                            forceToFront(TodoTimerApp.this);
                        }
                    });
                }
            }
        });

        // 오버레이 창에서 포커스가 빠져나가는 경우도 동일하게 감시
        focusOverlay.addWindowFocusListener(new WindowFocusListener() {
            @Override public void windowGainedFocus(WindowEvent e) {}
            @Override
            public void windowLostFocus(WindowEvent e) {
                // 메인 창으로 이동하는 정상적인 전환은 이탈로 보지 않음
                if (e.getOppositeWindow() == TodoTimerApp.this) return;
                if (isRunning && focusModeEnabled) {
                    focusOverlay.incrementDistract();
                    new Thread(() -> playBeep()).start();
                    SwingUtilities.invokeLater(() -> forceToFront(focusOverlay));
                }
            }
        });
    }

    //  할 일 CRUD 기능
    private void addTodo() {
        String text = inputField.getText();
        if (text == null) text = "";
        text = text.trim();

        if (text.isEmpty()) {
            msg("할 일을 입력해주세요!");
            return;
        }
        // 글자 수가 너무 길면 화면이 깨질 수 있으므로 제한
        if (text.length() > 50) {
            msg("할 일 이름은 50자 이하로 입력해주세요!");
            return;
        }

        try {
            todoList.add(new TodoItem(text));
            listModel.addElement(todoList.get(todoList.size() - 1).toString());
            inputField.setText("");
            updateStatus();
        } catch (Exception ex) {
            ex.printStackTrace();
            msg("할 일을 추가하는 중 오류가 발생했습니다.");
        }
    }

    private void deleteTodo() {
        if (selectedIndex < 0 || selectedIndex >= todoList.size()) {
            msg("삭제할 항목을 선택해주세요!");
            return;
        }
        if (isRunning) {
            msg("타이머 실행 중에는 삭제할 수 없습니다!");
            return;
        }
        try {
            todoList.remove(selectedIndex);
            listModel.remove(selectedIndex);
            selectedIndex = -1;
            updateStatus();
        } catch (Exception ex) {
            ex.printStackTrace();
            msg("삭제 중 오류가 발생했습니다.");
        }
    }

    //  타이머 제어 (시작/정지/초기화/완료)
    private void startTimer() {
        if (selectedIndex < 0 || selectedIndex >= todoList.size()) {
            msg("타이머를 시작할 할 일을 선택해주세요!");
            return;
        }
        if (todoList.get(selectedIndex).isCompleted()) {
            msg("완료된 항목입니다. 다른 항목을 선택해주세요!");
            return;
        }

        sessionSeconds = 0;
        isBreakTime = false;
        isRunning = true;

        // 뽀모도로 모드면 집중 단계 목표 시간 설정
        if (isPomodoroMode) {
            pomodoroTargetSeconds = focusMinutes * 60L;
            pomodoroRound++;
            roundLabel.setText("라운드 " + pomodoroRound);
            pomodoroInfoLabel.setText("집중 " + focusMinutes + "분  →  휴식 " + breakMinutes + "분");
            pomodoroProgress.setValue(0);
            pomodoroProgress.setForeground(new Color(70, 160, 70));
        }

        try {
            swingTimer.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            msg("타이머를 시작할 수 없습니다. 다시 시도해주세요.");
            isRunning = false;
            return;
        }

        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        resetBtn.setEnabled(false);
        todoJList.setEnabled(false);
        statusLabel.setText("진행 중: " + todoList.get(selectedIndex).getName());

        // 집중 모드가 켜져 있으면 오버레이 표시
        if (focusModeEnabled) {
            focusOverlay.setTaskName(todoList.get(selectedIndex).getName());
            focusOverlay.setModeLabel(isPomodoroMode ? "뽀모도로 집중 모드" : "집중 모드");
            focusOverlay.updateTime("00:00:00");
            try {
                focusOverlay.setLocation(getLocation());
            } catch (Exception ignored) {
                // 위치 설정에 실패해도 오버레이 표시는 계속 진행
            }
            focusOverlay.setVisible(true);
        }
    }

    private void stopTimer() {
        try {
            swingTimer.stop();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        isRunning = false;
        isBreakTime = false;

        if (focusOverlay.isVisible()) {
            focusOverlay.setVisible(false);
        }

        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        resetBtn.setEnabled(true);
        todoJList.setEnabled(true);

        if (focusModeEnabled) {
            showFocusReport();
            focusOverlay.resetDistractCount();
        }

        statusLabel.setText("정지됨  |  할 일을 선택하고 시작하세요");
        refreshList();
    }

    private void resetTimer() {
        if (selectedIndex < 0 || selectedIndex >= todoList.size()) {
            msg("초기화할 항목을 선택해주세요!");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "'" + todoList.get(selectedIndex).getName() + "' 의 시간을 초기화할까요?",
            "초기화 확인", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            todoList.get(selectedIndex).resetTime();
            sessionSeconds = 0;
            pomodoroRound = 0;
            roundLabel.setText(" ");
            pomodoroInfoLabel.setText(" ");
            pomodoroProgress.setValue(0);
            timerLabel.setText("00:00:00");
            refreshList();
        }
    }

    private void toggleComplete() {
        if (selectedIndex < 0 || selectedIndex >= todoList.size()) {
            msg("항목을 선택해주세요!");
            return;
        }
        if (isRunning) {
            msg("타이머 실행 중에는 완료 처리할 수 없습니다!");
            return;
        }
        todoList.get(selectedIndex).toggleCompleted();
        refreshList();
        updateStatus();
    }

    //  집중 모드 / 뽀모도로 토글
    private void toggleFocusMode() {
        focusModeEnabled = focusModeToggle.isSelected();
        focusModeToggle.setText(focusModeEnabled ? "집중 모드 ON" : "집중 모드 OFF");
    }

    private void togglePomodoro() {
        isPomodoroMode = pomodoroToggle.isSelected();
        updatePomodoroSettings();
        if (isPomodoroMode) {
            pomodoroToggle.setText("뽀모도로 ON");
            pomodoroInfoLabel.setText("집중 " + focusMinutes + "분  →  휴식 " + breakMinutes + "분");
        } else {
            pomodoroToggle.setText("뽀모도로 OFF");
            pomodoroInfoLabel.setText(" ");
            roundLabel.setText(" ");
            pomodoroProgress.setValue(0);
        }
    }

    // 콤보박스에서 선택한 "25분" 같은 문자열을 정수로 변환
    // 잘못된 값이 들어와도 기본값(25분/5분)으로 안전하게 대체
    private void updatePomodoroSettings() {
        focusMinutes = parseMinutes(focusTimeCombo.getSelectedItem(), 25);
        breakMinutes = parseMinutes(breakTimeCombo.getSelectedItem(), 5);
        if (isPomodoroMode) {
            pomodoroInfoLabel.setText("집중 " + focusMinutes + "분  →  휴식 " + breakMinutes + "분");
        }
    }

    // "25분" 형식의 문자열에서 숫자만 추출. 실패 시 기본값 반환
    private int parseMinutes(Object item, int defaultValue) {
        if (item == null) return defaultValue;
        try {
            String s = item.toString().replace("분", "").trim();
            int val = Integer.parseInt(s);
            return (val > 0) ? val : defaultValue;
        } catch (NumberFormatException ex) {
            // 콤보박스 값이 손상되었거나 예상치 못한 형식이면 기본값 사용
            return defaultValue;
        }
    }

    // 뽀모도로 집중/휴식 단계가 끝났을 때 호출되는 처리
    private void handlePomodoroPhaseEnd() {
        swingTimer.stop();
        sessionSeconds = 0;

        if (!isBreakTime) {
            // 집중 단계 종료 → 휴식 단계로 전환
            playBeep();
            isBreakTime = true;
            pomodoroTargetSeconds = breakMinutes * 60L;
            pomodoroProgress.setValue(0);
            pomodoroProgress.setForeground(new Color(60, 120, 210));
            if (focusModeEnabled && focusOverlay.isVisible()) {
                focusOverlay.setModeLabel("휴식 시간");
                focusOverlay.updateTime("00:00:00");
            }
            JOptionPane.showMessageDialog(this,
                pomodoroRound + "라운드 집중 완료!\n" + breakMinutes + "분 휴식을 시작합니다.",
                "집중 완료!", JOptionPane.INFORMATION_MESSAGE);
            swingTimer.start();
        } else {
            // 휴식 단계 종료 → 다음 라운드 진행 여부 확인
            isBreakTime = false;
            playBeep();
            pomodoroTargetSeconds = focusMinutes * 60L;
            pomodoroProgress.setValue(0);
            pomodoroProgress.setForeground(new Color(70, 160, 70));
            if (focusModeEnabled && focusOverlay.isVisible()) {
                focusOverlay.setModeLabel("집중 모드");
            }

            int choice = JOptionPane.showConfirmDialog(this,
                "휴식 완료!\n다음 라운드를 시작할까요?",
                "휴식 완료", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                pomodoroRound++;
                roundLabel.setText("라운드 " + pomodoroRound);
                swingTimer.start();
            } else {
                stopTimer();
            }
        }
    }

    // 집중 모드 종료 시 집중도 리포트 표시
    private void showFocusReport() {
        int d = focusOverlay.getDistractCount();
        int f = Math.max(0, 100 - d * 10); // 이탈 1회당 10점 감점, 최저 0점
        String g = f >= 90 ? "매우 집중" : f >= 70 ? "양호" : f >= 50 ? "보통" : "집중 필요";
        JOptionPane.showMessageDialog(this,
            "집중 리포트\n\n이탈 횟수: " + d + "회\n집중도: " + f + "%\n평가: " + g,
            "집중 결과", JOptionPane.INFORMATION_MESSAGE);
    }

    // 초 단위 시간을 HH:MM:SS 문자열로 변환
    private String fmt(long sec) {
        return String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
    }

    //  데이터 저장 / 불러오기
    //  저장 형식 (todo_data.txt, UTF-8):
    //    DATE=2025-06-21
    //    FOCUS_MODE=true/false
    //    POMODORO=true/false
    //    FOCUS_MIN=25
    //    BREAK_MIN=5
    //    COUNT=항목 개수
    //    이름|누적초|완료여부   (COUNT만큼 반복)
    private File getSaveFile() {
        return new File("todo_data.txt");
    }

    // 현재 상태를 파일에 저장. 실패해도 앱이 죽지 않도록 예외를 잡아서 처리
    private void saveData() {
        File saveFile = getSaveFile();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(saveFile), "UTF-8"))) {

            pw.println("DATE=" + LocalDate.now().toString());
            pw.println("FOCUS_MODE=" + focusModeEnabled);
            pw.println("POMODORO=" + isPomodoroMode);
            pw.println("FOCUS_MIN=" + focusMinutes);
            pw.println("BREAK_MIN=" + breakMinutes);
            pw.println("COUNT=" + todoList.size());

            for (TodoItem item : todoList) {
                // 이름에 구분자('|')가 포함되면 데이터가 깨질 수 있어 다른 문자로 치환
                String safeName = item.getName().replace("|", "/");
                pw.println(safeName + "|" + item.getAccumulatedSeconds() + "|" + item.isCompleted());
            }

        } catch (IOException ex) {
            // 저장 실패는 흔히 권한 문제나 디스크 문제이므로 사용자에게 알림
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "데이터 저장에 실패했습니다.\n파일 접근 권한을 확인해주세요.\n(" + saveFile.getAbsolutePath() + ")",
                "저장 오류", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            // 예상치 못한 오류도 앱 종료를 막지 않도록 잡아둠
            ex.printStackTrace();
        }
    }

    // 저장된 파일을 읽어 상태 복원. 파일이 없거나 형식이 깨져도 앱은 정상 시작됨
    private void loadData() {
        File f = getSaveFile();
        if (!f.exists()) return; // 처음 실행이면 그냥 빈 상태로 시작

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), "UTF-8"))) {

            String dateLine     = br.readLine();
            String focusLine    = br.readLine();
            String pomodoroLine = br.readLine();
            String focusMinLine = br.readLine();
            String breakMinLine = br.readLine();
            String countLine    = br.readLine();

            // 핵심 정보(날짜, 개수)가 없으면 파일이 손상된 것으로 보고 중단
            if (dateLine == null || countLine == null) {
                System.err.println("저장 파일 형식이 올바르지 않아 불러오기를 건너뜁니다.");
                return;
            }

            String savedDate = safeStrip(dateLine, "DATE=");
            String todayStr  = LocalDate.now().toString();
            boolean isToday  = savedDate.equals(todayStr);

            // 집중 모드 상태 복원 (값이 없거나 잘못되어도 기본값 false 유지)
            if (focusLine != null) {
                focusModeEnabled = parseBoolSafe(safeStrip(focusLine, "FOCUS_MODE="), false);
                focusModeToggle.setSelected(focusModeEnabled);
                focusModeToggle.setText(focusModeEnabled ? "집중 모드 ON" : "집중 모드 OFF");
            }

            // 뽀모도로 상태 복원
            if (pomodoroLine != null) {
                isPomodoroMode = parseBoolSafe(safeStrip(pomodoroLine, "POMODORO="), false);
                pomodoroToggle.setSelected(isPomodoroMode);
                pomodoroToggle.setText(isPomodoroMode ? "뽀모도로 ON" : "뽀모도로 OFF");
            }

            // 집중/휴식 시간 복원 (잘못된 값이면 기존 기본값 유지)
            if (focusMinLine != null) {
                focusMinutes = parseIntSafe(safeStrip(focusMinLine, "FOCUS_MIN="), focusMinutes);
                focusTimeCombo.setSelectedItem(focusMinutes + "분");
            }
            if (breakMinLine != null) {
                breakMinutes = parseIntSafe(safeStrip(breakMinLine, "BREAK_MIN="), breakMinutes);
                breakTimeCombo.setSelectedItem(breakMinutes + "분");
            }
            if (isPomodoroMode) {
                pomodoroInfoLabel.setText("집중 " + focusMinutes + "분  →  휴식 " + breakMinutes + "분");
            }

            // 할 일 목록 복원
            int count = parseIntSafe(safeStrip(countLine, "COUNT="), 0);
            todoList.clear();
            listModel.clear();

            int loadedCount = 0;
            for (int i = 0; i < count; i++) {
                String line = br.readLine();
                if (line == null) break; // 파일이 중간에 끊긴 경우 더 이상 읽지 않음

                String[] parts = line.split("\\|");
                if (parts.length < 3) {
                    // 형식이 안 맞는 줄은 건너뛰고 계속 진행 (전체 로딩이 중단되지 않게)
                    continue;
                }

                try {
                    TodoItem item = new TodoItem(parts[0]);
                    long seconds = parseLongSafe(parts[1], 0);
                    boolean completed = parseBoolSafe(parts[2], false);

                    // 같은 날 데이터일 때만 누적 시간을 복원, 날짜가 바뀌면 0으로 시작
                    if (isToday) item.setAccumulatedSeconds(seconds);
                    if (completed) item.toggleCompleted();

                    todoList.add(item);
                    listModel.addElement(item.toString());
                    loadedCount++;
                } catch (Exception lineEx) {
                    // 한 줄 단위 오류는 건너뛰고 나머지 데이터는 정상적으로 불러옴
                    System.err.println("항목을 불러오는 중 오류 (무시하고 계속): " + line);
                }
            }

            if (!isToday && loadedCount > 0) {
                JOptionPane.showMessageDialog(this,
                    savedDate + " 에 저장된 데이터를 불러왔습니다.\n(새 날이므로 누적 시간은 초기화되었습니다)",
                    "데이터 불러오기", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "데이터 파일을 읽는 중 오류가 발생했습니다.\n새로운 상태로 시작합니다.",
                "불러오기 오류", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            // 예상치 못한 오류가 나도 앱은 정상적으로 시작되어야 함
            ex.printStackTrace();
        }
    }

    // 안전한 파싱 보조 메서드
    // "KEY=value" 형식에서 KEY= 부분을 제거. null이면 빈 문자열 반환
    private String safeStrip(String line, String prefix) {
        if (line == null) return "";
        return line.startsWith(prefix) ? line.substring(prefix.length()).trim() : line.trim();
    }

    private boolean parseBoolSafe(String s, boolean defaultValue) {
        if (s == null || s.trim().isEmpty()) return defaultValue;
        String t = s.trim();
        if (t.equalsIgnoreCase("true")) return true;
        if (t.equalsIgnoreCase("false")) return false;
        return defaultValue; // true/false 외의 값이면 기본값 사용
    }

    private int parseIntSafe(String s, int defaultValue) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private long parseLongSafe(String s, long defaultValue) {
        try {
            long v = Long.parseLong(s.trim());
            return Math.max(0, v); // 음수 누적시간은 의미가 없으므로 0으로 보정
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    //  화면 갱신 / 상태 표시
    private void refreshList() {
        int cur = todoJList.getSelectedIndex();
        listModel.clear();
        for (TodoItem item : todoList) {
            listModel.addElement(item.toString());
        }
        // 선택 인덱스가 유효 범위 안이면 선택 상태 복원
        if (cur >= 0 && cur < listModel.size()) {
            todoJList.setSelectedIndex(cur);
        }
    }

    private void updateStatus() {
        if (selectedIndex >= 0 && selectedIndex < todoList.size()) {
            TodoItem item = todoList.get(selectedIndex);
            statusLabel.setText("선택: " + item.getName() + "  |  누적: " + item.getFormattedTime()
                + (item.isCompleted() ? "  (완료)" : ""));
        } else {
            statusLabel.setText("할 일을 선택하고 타이머를 시작하세요");
        }
    }

    private void msg(String text) {
        JOptionPane.showMessageDialog(this, text, "알림", JOptionPane.WARNING_MESSAGE);
    }

    // 할 일 목록의 각 줄을 그리는 커스텀 렌더러 (완료 항목은 회색으로 표시)
    class TodoCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setBorder(new EmptyBorder(6, 10, 6, 10));
            if (index >= 0 && index < todoList.size()) {
                TodoItem item = todoList.get(index);
                setForeground(isSelected ? Color.WHITE : (item.isCompleted() ? new Color(160, 165, 180) : Color.BLACK));
                setFont(new Font("맑은 고딕", Font.PLAIN, 13));
                if (isSelected) setBackground(new Color(65, 115, 195));
            }
            return this;
        }
    }

    //  프로그램 진입점
    public static void main(String[] args) {
        // Swing UI는 반드시 Event Dispatch Thread에서 생성해야 함
        SwingUtilities.invokeLater(() -> {
            try {
                new TodoTimerApp();
            } catch (Exception ex) {
                // 앱 초기화 도중 예외가 발생해도 콘솔에 원인을 남기고
                // 사용자에게도 알림을 띄움 (조용히 죽지 않도록)
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "프로그램을 시작하는 중 오류가 발생했습니다.\n" + ex.getMessage(),
                    "시작 오류", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
