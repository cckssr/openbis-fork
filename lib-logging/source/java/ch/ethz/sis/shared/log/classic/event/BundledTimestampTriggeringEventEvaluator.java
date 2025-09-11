/*
 *  Copyright ETH 2007 - 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package ch.ethz.sis.shared.log.classic.event;



import java.util.Date;
import java.util.logging.LogRecord;

/**
 * A {@link BundledTimestampTriggeringEventEvaluator} that triggers every 10 minutes. Can be used, e.g. to make the
 * {@link org.apache.log4j.net.SMTPAppender} send email also on non-error conditions.; REMARK: This is a solution for Mario Emmenlauer. It should not
 * be recommended because it can lead to the fact that important log events aren't sent for days because no triggering event appears.
 * 
 * @author Althea Parker
 */
public class BundledTimestampTriggeringEventEvaluator
{

    private long timestamp;

    public static long timePeriod = 10 * 60000;

    public BundledTimestampTriggeringEventEvaluator()
    {
        timestamp = new Date().getTime();
    }

    public boolean isTriggeringEvent(LogRecord record) {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= timestamp + timePeriod) {
            timestamp = currentTime;
            return true;
        }
        return false;
    }

}
