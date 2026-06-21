// TodoItem.java - 할 일 데이터 모델
public class TodoItem {
    private String name;             // 할 일 이름
    private long accumulatedSeconds; // 누적 작업 시간(초)
    private boolean completed;       // 완료 여부

    public TodoItem(String name) {
        // 이름이 null이거나 비어있으면 기본값으로 대체 (방어적 처리)
        this.name = (name == null || name.trim().isEmpty()) ? "이름없음" : name.trim();
        this.accumulatedSeconds = 0;
        this.completed = false;
    }

    public String getName() { return name; }
    public boolean isCompleted() { return completed; }
    public long getAccumulatedSeconds() { return accumulatedSeconds; }

    // 누적 시간을 직접 설정 (파일 불러오기용). 음수가 들어오면 0으로 보정
    public void setAccumulatedSeconds(long s) {
        this.accumulatedSeconds = Math.max(0, s);
    }

    // 누적 시간에 초 단위로 더함
    public void addSeconds(long s) {
        if (s > 0) this.accumulatedSeconds += s;
    }

    public void resetTime() { accumulatedSeconds = 0; }
    public void toggleCompleted() { completed = !completed; }

    // 누적 시간을 HH:MM:SS 형식 문자열로 변환
    public String getFormattedTime() {
        long h = accumulatedSeconds / 3600;
        long m = (accumulatedSeconds % 3600) / 60;
        long s = accumulatedSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    @Override
    public String toString() {
        return (completed ? "[완료] " : "[  ]  ") + name + "  " + getFormattedTime();
    }
}
