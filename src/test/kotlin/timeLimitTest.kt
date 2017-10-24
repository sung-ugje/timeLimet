import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class TimeLimitTest {
    @Test
    fun checkTimeLimitAll() {
        isDebug = true
        val today = Calendar.getInstance()
        var parm = "1,3,4,5,6,7".split(",")
        var result = isAllowedWeek(today, parm)
        assertEquals(true, result, "허용된 요일이 아닙니다.")

        parm = "7,9-20".split(",")
        result = isAllowedTime(today, parm)
        assertEquals(true, result, "허용된 시간이 아닙니다.")

        parm = "190,1:50,7:50".split(",")
        result = overLimitTime(today, parm,"mk")
        assertEquals(false, result, "사용시간을 초과 하였습니다.")
    }
}