import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.io.FileInputStream
import java.lang.System.exit
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*

/**
 * 사용자 아이디, 1일 사용시간, 허용 시간, 허용 요일
 */
fun main(args:Array<String>) {
    if (args.size < 2) {
        println("ERROR : TimeLimit is use 2 parameter. \nAdd parameter : User name (String) and limit time (int minute)")
        return
    }
    var prop = Properties()
    prop.load(FileInputStream("global.properties"))
    var lt = prop.get("limit.time").toString().split(":")
    val userid = prop.get("userid").toString()
    var limitTime:Int
    try {
        limitTime = lt[0].toInt()*60+lt[1].toInt()
    } catch (e:NumberFormatException){
        limitTime = 40
    }
    var lastStr:String
    var item:List<String>
    val today:Calendar = Calendar.getInstance()
    val sf = SimpleDateFormat("MMM dd yyyy")
    while(true){
        lastStr = Runtime.getRuntime().exec("last -R $userid").inputStream.bufferedReader().readText()
        var intime:Calendar = Calendar.getInstance() // last 에서 읽어들인 시간

        var sdf = SimpleDateFormat("yyyyMMdd")
        var runtime = 0  // 플레이 시간
        for (node in lastStr.split("\n")){
            item = node.split("[\\s]+".toRegex())
            if(item[0]== userid ){
                intime.setTime(sf.parse("${item[4]} "))
            //}, ${item[4]} ${today.get(Calendar.YEAR)} ${item[5]}")

                if(sdf.format(intime) == sdf.format(today.time)){
                    if(item[6] == "still"){

                        runtime += (today.timeInMillis - intime.timeInMillis).toInt() / 60000
                    } else {
                        var rtm = item[8].replace("(","").replace(")","").split(":")
                        if(rtm[0].indexOf("+") > -1) {
                            runtime += rtm[0].split("+")[0].toInt() * 60 * 24
                            runtime += rtm[0].split("+")[1].toInt() * 60
                        } else {
                            runtime += rtm[0].toInt() * 60
                        }
                        runtime += rtm[1].toInt()
                    }
                }
            }
        }
        if(runtime > limitTime){
            println("Today limite time is Run Time : $limitTime minus, but run time is $runtime minus \nSorry, I kill you all process")
            Runtime.getRuntime().exec("pkill -9 -U $userid")
        } else {
            if (limitTime - runtime < 5){
                makeAlert("컴퓨터 사용시간이 ${(limitTime - runtime).toString()} 분 남았습니다. " )

            }
            println("Today limite time is Run Time : $limitTime minus, but run time is $runtime minus ")
        }
        sleep(60000)
    }
}
fun makeAlert(msg:String){
    var frame = JFrame("사용시간제한 알림")
    frame.setBounds(10,20,500,200)
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(null)
    val panel = JPanel()
    panel.setBounds(10,30,460,50)
    //panel.backgr
    // ound = Color(255,255,0)
    val label = JLabel(msg)
    label.font = Font("굴림",Font.BOLD,20)
    panel.add(label)
    frame.add(panel)
    val button = JButton("Close")
    button.setBounds(210,100,80,30)
    //button.addAncestorListener()
    button.addActionListener(object : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            frame.isVisible = false
        }
    })
    frame.add(button)
    frame.isVisible = true
    sleep(10000)
    frame.isVisible = false
}
fun  getProp(){
    var prop = Properties()
    prop.load(FileInputStream("global.properties"))
    var limitAT=prop.get("allowed.time")
    var limitAD=prop.get("allowed.dayofweek")
    var limitTM=prop.get("limit.time")
    var limitUI=prop.get("userid")

}
        /*
public String getProp() {
    Properties properties = new Properties();
    properties.load(new FileInputStream("example01.properties"));
    return  properties.getProperty("a");
}
*/