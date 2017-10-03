import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 사용자 아이디, 1일 사용시간, 허용 시간, 허용 요일
 * global.properties : 모든 설정을 저장하고 있는프로퍼티 화일
 *  - limit.time : 하루에 사용할 수 있는 시간을 분단위로 저장한다. , 구분자를 두어서 요일별 시간할당을 할수 있다.
 *  - userid : 사용 제한할 유닉스 계정을 저장한다.
 *  - allowed.dayofweek : 허용요일 - 구분자를 ,로 구분하여 1,2,3,6 같은 형태로 저장한다 만약 지정하지 않으면 모든 요일을 허용하게 된다. (1:일요일,2:월요일,3,화요일 ... 7:토요일)
 *  - allowed.time : 허용시간 - 구분자를 ,로 구분하여 허용할 시간을 지정한다. 시간은 24시간제를 사용하며 지정지 않으면 모든 시간이 허용된다.
 *                  시간지정은 시작시간과 종료시간을 시분으로 표시하며 분을 지정하지 않으면 시작분은 0 분 종료분은 59분으로 인식한다.
 *                  시작시간과 종료시간을 지정하지 안흐면 지정한 시간만 인식하게 된다.
 *                  EX) 7-9,12:30-15:50,17,19 --> 오전 7시0분 부터 9시 59분까지, 12시30분 부터 오후 3시50분까지, 오후 5시 0분-59분, 오후 7시0분-59분 까지 사용이 가능하다.
 *  적용순서는 허용 요일 -> 허용 시간 -> 사용가능시간 순으로 체크하게 된다.
 */
var isDebug = false

fun main(args: Array<String>) {

    val prop = Properties()
    prop.load(FileInputStream("/etc/timelimit.conf"))
    val limitTime = prop.get("limit.time").toString().split(",")  // 하루 사용 제한 시간
    val userid = prop.get("userid").toString()  // 사용자 아이디
    val aweek = prop.get("allowed.dayofweek").toString().split(",")  // 허용되는  요일 리스트
    val atime = prop.get("allowed.time").toString().split(",")  // 1,2-3,4:00-12:00
    val today:Calendar = Calendar.getInstance()
    if ( isDebug ) {println("Conf val ==> limitTime: $limitTime , userid = $userid,  aweek = $aweek,  atime = $atime,  today = $today ")}
    if (! isAllowedWeek(today = today, aweek = aweek)) {
        Runtime.getRuntime().exec("pkill -9 -U $userid")
    }
    if (! isAllowedTime(today = today, atime = atime)) {
        Runtime.getRuntime().exec("pkill -9 -U $userid")
    }
    if (overLimitTime(today,limitTime,userid) ) {
        Runtime.getRuntime().exec("pkill -9 -U $userid")
    }
}

/**
 * Swing 으로 화면에 경고창을 발생 시킨다.
 */
fun makeAlert(msg:String){
    Runtime.getRuntime().exec("sendMsgMK.py $msg")
}

/**
 * 허용 요일 처리 루틴
 */
fun isAllowedWeek(today:Calendar, aweek:List<String>):Boolean {
    if (aweek.isEmpty()) return true
    val week = today.get(Calendar.DAY_OF_WEEK)
    if ( isDebug ) println("Today week $week.")
    if ( aweek.contains("$week")) {  // 허용요일에 오늘이 포함되어 있으면
        if ( isDebug ) println("Today is allowed day of week.")
        return true
    } else {
        if ( isDebug ) println("Today is not allowed day of week.")
        return false
    }
}

fun isAllowedTime(today:Calendar, atime:List<String>):Boolean{
    if(atime.isEmpty()) return true
    var startTm:Int
    var endTm:Int
    val current = today.get(Calendar.HOUR_OF_DAY) * 60 + today.get(Calendar.MINUTE)
    var rtn = false

    atime.forEach { allowedTime ->
        if (allowedTime.isNotEmpty()) {
            if (allowedTime.contains("-")) {
                if (allowedTime.contains(":")) {
                    startTm = allowedTime.split("-")[0].split(":")[0].toInt() * 60
                    startTm += allowedTime.split("-")[0].split(":")[1].toInt()
                    endTm = allowedTime.split("-")[1].split(":")[0].toInt() * 60
                    endTm += allowedTime.split("-")[1].split(":")[1].toInt()
                } else {
                    startTm = allowedTime.split("-")[0].toInt() * 60
                    endTm = allowedTime.split("-")[1].toInt() * 60 + 59
                }
            } else {
                if (allowedTime.contains(":")) {
                    startTm = allowedTime.split(":")[0].toInt() * 60
                    startTm += allowedTime.split(":")[1].toInt()
                    endTm = startTm
                } else {
                    startTm = allowedTime.toInt() * 60
                    endTm = startTm + 60
                }
            }
            if ( isDebug ) println("tolen time  $startTm >= $current <= $endTm")
            if (current >= startTm && endTm <= endTm) rtn = true
        }
    }
    if ( isDebug ) println("Today is not allowed Time. $rtn")
    return rtn
}

fun overLimitTime(today:Calendar, limitTime:List<String>, userid:String):Boolean{
    if ( isDebug ) println("in fun overLimitTime (limitTime : $limitTime, userid : $userid, today: $today")
    if (limitTime.isEmpty()) return true
    var limit = limitTime[0].toInt()  // 하루 사용 제한 시간
    if ( isDebug ) println("default limitTime : $limit")
    limitTime.forEach{ limittm ->
        if(limittm.contains(":")) {
            if(limittm.split(":")[0] == today.get(Calendar.DAY_OF_WEEK).toString()){
                limit = limittm.split(":")[1].toInt()
            }
        }
    }
    if ( isDebug ) println("comfirm limitTime : $limit")
    var item:List<String>
    val sf = SimpleDateFormat("MMM/dd/yyyy HH:mm",Locale.ENGLISH)
    //val lastStr = Runtime.getRuntime().exec("last -R $userid").inputStream.bufferedReader().readText()
    val lastStr ="""mk       :0           Tue Oct  3 12:22   still logged in
mk       :0           Tue Oct  3 12:21 - 12:21  (00:00)
mk       :0           Tue Oct  3 12:17 - 12:17  (00:00)
mk       :0           Tue Oct  3 12:16 - 12:16  (00:00)
mk       :0           Tue Oct  3 12:11 - 12:11  (00:00)
mk       :0           Tue Oct  3 12:10 - 12:10  (00:00)
mk       :0           Tue Oct  3 12:10 - 12:10  (00:00)
mk       :0           Tue Oct  3 12:09 - 12:09  (00:00)
mk       :0           Tue Oct  3 12:09 - 12:09  (00:00)
mk       :0           Tue Oct  3 12:09 - 12:09  (00:00)
mk       :0           Tue Oct  3 12:08 - 12:08  (00:00)
mk       :0           Tue Oct  3 12:04 - 12:08  (00:03)
mk       :0           Tue Oct  3 12:02 - 12:03  (00:00)
mk       :0           Tue Oct  3 10:47 - 10:47  (00:00)
mk       :0           Tue Oct  3 10:47 - 10:47  (00:00)
mk       :0           Tue Oct  3 10:46 - 10:46  (00:00)
mk       :0           Tue Oct  3 06:49 - 08:18  (01:28)
mk       :0           Mon Oct  2 19:05 - 19:29  (00:23)
mk       :0           Mon Oct  2 19:04 - 19:04  (00:00)
mk       :0           Mon Oct  2 18:08 - 19:04  (00:55)
mk       :0           Mon Oct  2 13:07 - 13:08  (00:01)
mk       :0           Mon Oct  2 06:53 - 07:44  (00:50)
mk       :0           Sun Oct  1 06:33 - 07:23  (00:50)"""
    if ( isDebug ) println("last \n $lastStr")

    val intime = Calendar.getInstance() // last 에서 읽어들인 시간
    val sdf = SimpleDateFormat("yyyyMMdd")
    var runtime: Int = 0    // 플레이 시간
    var rtm:List<String>
    lastStr.split("\n").forEach { node ->
        item = node.split(regex = "[\\s]+".toRegex())
        if ( isDebug ) print("Last Data userid : ${item[0]}")
        if(item[0] == userid){
            if ( isDebug ) print(", intime : ${item[3]}/${item[4]}/2017")
            intime.setTime(sf.parse("${item[3]}/${item[4]}/2017 ${item[5]}"))
            if(sdf.format(intime.time) == sdf.format(today.time)){
                if ( isDebug ) print(", item : ${item[6]}")
                if(item[6] == "still"){
                    runtime += (today.timeInMillis - intime.timeInMillis).toInt() / 60000
                } else {
                    rtm = item[8].replace("(","").replace(")","").split(":")
                    if(rtm[0].indexOf("+") > -1) {  // 하루가 넘어가면 + 로 구분한다.
                        runtime += rtm[0].split("+")[0].toInt() * 60 * 24
                        runtime += rtm[0].split("+")[1].toInt() * 60
                    } else {
                        runtime += rtm[0].toInt() * 60
                    }
                    runtime += rtm[1].toInt()
                }
                if ( isDebug ) println(", runtime : $runtime")

            }
        }
    }
    if ( isDebug ) println("total runtime : $runtime")
    if(runtime > limit){
        if ( isDebug ) println ("Today limite time is Run Time : $limit minus, but run time is $runtime minus \nSorry, I kill you all process")
        return true
    } else {
        if (limit - runtime < 5){
            makeAlert("You have ${(limit - runtime).toString()} minutes left on your computer." )
            if ( isDebug ) println ("You have ${(limit - runtime).toString()} minutes left on your computer.")
        }
        if ( isDebug ) println ("Time limit pass")
        return false
    }

}