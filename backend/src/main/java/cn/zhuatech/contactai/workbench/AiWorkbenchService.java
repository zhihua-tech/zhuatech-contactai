/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.contactai.workbench;

import static cn.zhuatech.contactai.workbench.WorkbenchInput.*;

import java.math.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
@Service
public class AiWorkbenchService {
  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  public Map<String, Object> execute(Map<String, Object> r) {
    validate(r);
    keys(r, "durationSeconds", "turns");
    int duration = integer(r, "durationSeconds", 1, 86400);
    var turns = new ArrayList<>(rows(r, "turns", 1, 500));
    Set<String> ids = new HashSet<>();
    for (var t : turns) {
      keys(t, "id", "speaker", "start", "end");
      unique(ids, id(t, "id"));
      if (!Set.of("AGENT", "CUSTOMER").contains(text(t, "speaker", 10)))
        throw new IllegalArgumentException("发言人类型错误");
      int a = integer(t, "start", 0, duration), b = integer(t, "end", 0, duration);
      if (a >= b) throw new IllegalArgumentException("发言结束时间必须晚于开始时间");
    }
    turns.sort(
        Comparator.<Map<String, Object>, Integer>comparing(
                t -> ((Number) t.get("start")).intValue())
            .thenComparing(t -> t.get("id").toString()));
    long talk = 0, covered = 0;
    int unionEnd = 0, waitingEnd = -1;
    List<Map<String, Object>> responses = new ArrayList<>();
    for (var t : turns) {
      int a = ((Number) t.get("start")).intValue(), b = ((Number) t.get("end")).intValue();
      talk += b - a;
      covered += Math.max(0, b - Math.max(a, unionEnd));
      unionEnd = Math.max(unionEnd, b);
      if (t.get("speaker").equals("CUSTOMER")) waitingEnd = Math.max(waitingEnd, b);
      else if (waitingEnd >= 0) {
        responses.add(Map.of("agentTurn", t.get("id"), "gapSeconds", Math.max(0, a - waitingEnd)));
        waitingEnd = -1;
      }
    }
    double average =
        responses.stream().mapToInt(x -> (Integer) x.get("gapSeconds")).average().orElse(0);
    return Map.of(
        "durationSeconds",
        duration,
        "speechSeconds",
        covered,
        "silenceSeconds",
        duration - covered,
        "overlapSeconds",
        talk - covered,
        "responseCount",
        responses.size(),
        "meanResponseGapSeconds",
        round(average),
        "responses",
        responses);
  }
}
