package ch.openbis.drive.protobuf.converters;

import ch.openbis.drive.model.Event;
import ch.openbis.drive.protobuf.DriveApiService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class EventClientDtoTest {
    @Test
    public void test() {
        DriveApiService.Event protobufEvent = DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.UP)
                .setLocalFile("/loc").setDirectory(true).setSourceDeleted(false).setRemoteFile("/rem").setTimestamp(432523L).build();

        EventClientDto eventClientDto = new EventClientDto(protobufEvent);
        Assert.assertEquals(Event.SyncDirection.UP, eventClientDto.getSyncDirection());
        Assert.assertEquals("/loc", eventClientDto.getLocalFile());
        Assert.assertEquals(true, eventClientDto.isDirectory());
        Assert.assertEquals(false, eventClientDto.isSourceDeleted());
        Assert.assertEquals("/rem", eventClientDto.getRemoteFile());
        Assert.assertEquals((Long) 432523L, eventClientDto.getTimestamp());
    }
}