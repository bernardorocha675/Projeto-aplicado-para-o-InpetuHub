package br.com.inpetu.android;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;

/** Port explícito das regras de DisponibilidadeService/TaxacaoService/PrecoService, base 0.4.5. */
public final class Rules {
 private Rules() {}
 public static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
 public static String text(String value,int max,String label){String s=value==null?"":value.trim();require(!s.isEmpty()&&s.length()<=max,"Confira: "+label);return s;}
 public static int integer(String value,String label){try{require(value!=null&&value.matches("[0-9]+"),label+" deve ser inteiro positivo.");int n=Integer.parseInt(value);require(n>0,label+" deve ser positivo.");return n;}catch(NumberFormatException e){throw new IllegalArgumentException(label+" fora do limite.");}}
 public static boolean overlap(int a,int b,int c,int d){return a<d&&b>c;}
 public static int minute(String value){try{return LocalTime.parse(value).getHour()*60+LocalTime.parse(value).getMinute();}catch(Exception e){throw new IllegalArgumentException("Informe o horário como HH:mm.");}}
 public static String time(int minutes){return String.format(java.util.Locale.ROOT,"%02d:%02d",minutes/60,minutes%60);}
 public static void schedule(LocalDate day,int start,int end,int count,int capacity,int open,int close,LocalDateTime now){
  require(end>start&&start>=open&&end<=close,"Confira o horário dentro do expediente: "+time(open)+"–"+time(close));
  require(!day.atTime(start/60,start%60).isBefore(now),"Não é possível reservar no passado.");
  require(!day.isAfter(now.toLocalDate().plusDays(365)),"Reservas limitadas aos próximos 365 dias.");
  require(count>0&&count<=capacity,"Participantes devem ficar entre 1 e "+capacity+".");
 }
 public static int[] taxes(String code,boolean quota,boolean paid,boolean academic){
  if(code.equals("P1"))return new int[]{0,0,0,0};
  if(!quota)return new int[]{100,100,100,0};
  if(code.equals("P2")){if(paid)return new int[]{50,100,100,0};if(academic)return new int[]{0,0,100,1};return new int[]{100,100,100,0};}
  return new int[]{paid?75:0,100,100,0};
 }
 public static long money(String raw){try{BigDecimal value=new BigDecimal(raw.trim().replace(',','.'));require(value.signum()>=0&&value.scale()<=2&&value.compareTo(new BigDecimal("10000000"))<=0,"Valor deve ter até duas casas e ficar entre 0 e 10 milhões.");return value.movePointRight(2).longValueExact();}catch(NumberFormatException|ArithmeticException e){throw new IllegalArgumentException("Informe um valor válido, por exemplo 100,00.");}}
 public static long percent(long cents,int percentage){return BigDecimal.valueOf(cents).multiply(BigDecimal.valueOf(percentage)).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP).longValueExact();}
 public static int deposit(LocalDate day,boolean holiday){return day.getDayOfWeek()==DayOfWeek.SUNDAY||holiday?60:30;}
}
