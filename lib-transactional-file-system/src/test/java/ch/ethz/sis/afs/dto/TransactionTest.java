package ch.ethz.sis.afs.dto;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;

import ch.ethz.sis.afs.dto.operation.CopyOperation;
import ch.ethz.sis.afs.dto.operation.CreateOperation;
import ch.ethz.sis.afs.dto.operation.DeleteOperation;
import ch.ethz.sis.afs.dto.operation.FreeOperation;
import ch.ethz.sis.afs.dto.operation.HashOperation;
import ch.ethz.sis.afs.dto.operation.ListOperation;
import ch.ethz.sis.afs.dto.operation.MoveOperation;
import ch.ethz.sis.afs.dto.operation.Operation;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.dto.operation.PreviewOperation;
import ch.ethz.sis.afs.dto.operation.ReadOperation;
import ch.ethz.sis.afs.dto.operation.SnapshotOperation;
import ch.ethz.sis.afs.dto.operation.TruncateOperation;
import ch.ethz.sis.afs.dto.operation.WriteOperation;
import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;

public class TransactionTest
{

    @Test
    public void test_serialize_deserialize() throws Exception
    {
        Map<OperationName, OperationCreator> operationCreators = new HashMap<>();
        operationCreators.put(OperationName.List, () -> new ListOperation(UUID.randomUUID(), "testListSource", true));
        operationCreators.put(OperationName.Read, () -> new ReadOperation(UUID.randomUUID(), "testReadSource", 123, 456));
        operationCreators.put(OperationName.Write,
                () -> new WriteOperation(UUID.randomUUID(), "testWriteSource", "testWriteTempSource", 123, "testWriteData".getBytes()));
        operationCreators.put(OperationName.Delete, () ->
        {
            Path file = null;
            try
            {
                file = createTempFile();
                return new DeleteOperation(UUID.randomUUID(), file.toString(), true, "testDeleteTrashRoot");
            } finally
            {
                deleteFile(file);
            }
        });
        operationCreators.put(OperationName.Copy, () -> new CopyOperation(UUID.randomUUID(), "testCopySource", "testCopyTarget"));
        operationCreators.put(OperationName.Move, () -> new MoveOperation(UUID.randomUUID(), "testMoveSource", "testMoveTarget"));
        operationCreators.put(OperationName.Create, () -> new CreateOperation(UUID.randomUUID(), "testCreateSource", true));
        operationCreators.put(OperationName.Truncate, () -> new TruncateOperation(UUID.randomUUID(), "testTruncateSource", 123));
        operationCreators.put(OperationName.Snapshot, () -> new SnapshotOperation(UUID.randomUUID(), "testSnapshotSource"));
        operationCreators.put(OperationName.Free, () -> new FreeOperation(UUID.randomUUID(), "testFreeSource"));
        operationCreators.put(OperationName.Hash, () -> new HashOperation(UUID.randomUUID(), "testHashSource"));
        operationCreators.put(OperationName.Preview,
                () -> new PreviewOperation(UUID.randomUUID(), "testPreviewSource", Set.of("testPreviewFileType1", "testPreviewFileType2"), 123));

        List<Operation> operations = new ArrayList<>();
        for (OperationName operationName : OperationName.values())
        {
            OperationCreator operationCreator = operationCreators.get(operationName);
            if (operationCreator == null)
            {
                throw new RuntimeException("Operation creator for operation: " + operationName
                        + " is missing! Add it to the test to verify if the operation object is properly serialized and deserialized.");
            }
            operations.add(operationCreator.createOperation());
        }

        Transaction transaction = new Transaction("testWriteAheadLogRoot", "testStorageRoot", UUID.randomUUID(), operations);
        final byte[] bytes = JacksonObjectMapper.getInstance().writeValue(transaction);
        Transaction transactionDeserialized = JacksonObjectMapper.getInstance().readValue(new ByteArrayInputStream(bytes), Transaction.class);

        assertTrue(EqualsBuilder.reflectionEquals(transaction, transactionDeserialized));
    }

    private static Path createTempFile() throws IOException
    {
        return Files.createTempFile("transaction-test", "");
    }

    private static void deleteFile(Path file) throws IOException
    {
        if (file != null)
        {
            Files.deleteIfExists(file);
        }
    }

    private interface OperationCreator
    {

        Operation createOperation() throws Exception;

    }
}
