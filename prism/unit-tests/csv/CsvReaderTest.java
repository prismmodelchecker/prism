package csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;

import static csv.BasicReader.CR;
import static csv.BasicReader.LF;
import static csv.CsvReader.COMMA;
import static csv.CsvReader.CR_LF;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test whether {@link CsvReader} behaves according to to RFC 4180.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>
 */
public class CsvReaderTest
{
	// Test parameters

	@ParameterizedTest
	@ValueSource(ints = {',', ',', ' ', '\t'})
	public void testFieldSeparator(int eof) throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsDefault();

		BasicReader csv = getCsvDefault().convert(',').to(eof);

		CsvReader reader = new CsvReader(csv, false, true, true, eof, CR_LF);
		assertRecordsEquals(expected, reader);
	}

	@ParameterizedTest
	@ValueSource(ints = {CR, LF, CR_LF})
	public void testLineSeparator(int eol) throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsDefault();

		BasicReader csv = getCsvDefault();
		if (eol != CR_LF) {
			csv = csv.convert(CR, LF).to(eol);
		}

		CsvReader reader = new CsvReader(csv, false, true, true, COMMA, eol);
		assertRecordsEquals(expected, reader);
	}

	@Test
	public void testLineSeparatorErrorUnknown()
	{
		assertThrows(IllegalArgumentException.class, () -> new CsvReader(null, COMMA, 5));
	}

	@Test
	public void testFixNumFieldsTrue() throws IOException, CsvFormatException
	{
		CsvReader readerSame = new CsvReader(getCsvEmptyLastLine());
		assertDoesNotThrow(() -> readerSame.iterator().forEachRemaining(r -> {return;}));
	}

	@Test
	public void testFixNumFieldsTrueErrorDifferentNumberOfFields() throws IOException, CsvFormatException
	{
		CsvReader readerDiff = new CsvReader(getCsvDiffNumFields());
		assertThrows(CsvFormatException.class, () -> readerDiff.iterator().forEachRemaining(r -> {return;}));
	}

	@Test
	public void testFixNumFieldsFalse() throws IOException, CsvFormatException
	{
		CsvReader readerDiff = new CsvReader(getCsvDiffNumFields(), true, false, true);
		assertDoesNotThrow(() -> readerDiff.iterator().forEachRemaining(r -> {return;}));
	}

	@Test
	public void testHasHeaderTrue() throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsDefault();

		CsvReader reader = new CsvReader(getCsvDefault());
		assertArrayEquals(expected.remove(0), reader.getHeader());
		assertRecordsEquals(expected, reader);
	}

	@Test
	public void testHasHeaderTrueErrorEmptyCsv() throws IOException, CsvFormatException
	{
		assertThrows(CsvFormatException.class, () -> new CsvReader(getCsvEmpty()));

		assertThrows(CsvFormatException.class, () -> new CsvReader(getCsvNoRecords()));
	}

	@Test
	public void testHasHeaderTrueErrorNoRecords() throws IOException, CsvFormatException
	{
		assertThrows(CsvFormatException.class, () -> new CsvReader(getCsvNoRecords()));
	}

	@Test
	public void testHasHeaderFalse() throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsDefault();

		CsvReader reader = new CsvReader(getCsvDefault(), false, true, true);
		assertArrayEquals(null, reader.getHeader());
		assertRecordsEquals(expected, reader);

		assertDoesNotThrow(() -> new CsvReader(getCsvEmpty(), false, true, true));

		assertDoesNotThrow(() -> new CsvReader(getCsvNoRecords(), false, true, true));
	}

	@Test
	public void testDistinctFieldNamesTrue() throws IOException, CsvFormatException
	{
		assertDoesNotThrow(() -> new CsvReader(getCsvDefault()));
	}

	@Test
	public void testDistinctFieldNamesTrueErrorSameName() throws IOException, CsvFormatException
	{
		assertThrows(CsvFormatException.class, () -> new CsvReader(getCsvSameNames()));
	}

	@Test
	public void testDistinctFieldNamesFalse() throws IOException, CsvFormatException
	{
		BasicReader sameNames = getCsvSameNames();
		assertDoesNotThrow(() -> new CsvReader(sameNames, true, true, false));
	}

	// Test csv contents

	@Test
	public void testDoubleQuotes() throws IOException, CsvFormatException
	{
		BasicReader csv = getBasicReader("\"\"\"quo\"\"tes\"\"\",misc");
		CsvReader reader = new CsvReader(csv, false, true, true);
		assertArrayEquals(new String[] {"\"quo\"tes\"", "misc"}, reader.nextRecord());
		assertFalse(reader.hasNextRecord());
	}

	@Test
	public void testDoubleQuotesErrorInNonQuotedField() throws IOException, CsvFormatException
	{
		CsvReader reader = new CsvReader(getBasicReader("quo\"\"tes,misc"), false, true, true);
		assertThrows(CsvFormatException.class, reader::nextRecord);
	}

	@Test
	public void testDoubleQuotesErrorNotQuoted() throws IOException, CsvFormatException
	{
		CsvReader reader = new CsvReader(getBasicReader("\"quo\"tes,misc\""), false, true, true);
		assertThrows(CsvFormatException.class, reader::nextRecord);
	}


	@Test
	public void testEmptyCsv() throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = new ArrayList<>();
		expected.add(new String[]{""});

		CsvReader reader = new CsvReader(getCsvEmpty(), false, true, true);
		assertRecordsEquals(expected, reader);
	}

	@Test
	public void testEmptyFields() throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsEmptyFields();

		CsvReader reader = new CsvReader(getCsvEmptyFields());
		assertArrayEquals(expected.remove(0), reader.header);
		assertRecordsEquals(expected, reader);
	}

	@Test
	public void testEndWithEmptyLine() throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsDefault();

		CsvReader reader = new CsvReader(getCsvEmptyLastLine());
		assertArrayEquals(expected.remove(0), reader.getHeader());
		assertRecordsEquals(expected, reader);
	}

	@Test
	public void testEndWithoutNoEmptyLine() throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsDefault();

		CsvReader reader = new CsvReader(getCsvDefault());
		assertArrayEquals(expected.remove(0), reader.getHeader());
		assertRecordsEquals(expected, reader);
	}

	@Test
	public void testGetNumberOfFieldsNotFixed() throws IOException, CsvFormatException
	{
		CsvReader reader = new CsvReader(getCsvDefault(), false, false, true);
		assertEquals(-1, reader.getNumberOfFields());
		while (reader.hasNextRecord()) {
			reader.nextRecord();
			assertEquals(-1, reader.getNumberOfFields());
		}
	}

	@Test
	public void testGetNumberOfFieldsFixedWithHeader() throws IOException, CsvFormatException
	{
		CsvReader reader = new CsvReader(getCsvDefault());
		assertEquals(2, reader.getNumberOfFields());
		while (reader.hasNextRecord()) {
			reader.nextRecord();
			assertEquals(2, reader.getNumberOfFields());
		}
	}

	@Test
	public void testGetNumberOfFieldsFixedWithoutHeader() throws IOException, CsvFormatException
	{
		CsvReader readerFixedWithoutHeader = new CsvReader(getCsvDefault(), false, true, true);
		assertEquals(0, readerFixedWithoutHeader.getNumberOfFields());
		while (readerFixedWithoutHeader.hasNextRecord()) {
			readerFixedWithoutHeader.nextRecord();
			assertEquals(2, readerFixedWithoutHeader.getNumberOfFields());
		}
	}

	@Test
	public void testQuotedFields() throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsDefault();

		CsvReader reader = new CsvReader(getCsvQuotedFields());
		assertArrayEquals(expected.remove(0), reader.header);
		assertRecordsEquals(expected, reader);
	}

	@Test
	public void testQuotedFieldsComma() throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsComma();

		CsvReader reader = new CsvReader(getCsvComma());
		assertArrayEquals(expected.remove(0), reader.header);
		assertRecordsEquals(expected, reader);
	}

	@Test
	public void testQuotedFieldsCrLf() throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsCrLf();

		CsvReader reader = new CsvReader(getCsvCrLf());
		assertArrayEquals(expected.remove(0), reader.header);
		assertRecordsEquals(expected, reader);
	}

	@Test
	public void testQuotedFieldsErrorNotClosed() throws IOException, CsvFormatException
	{
		CsvReader reader = new CsvReader(getBasicReader("\"quotes,misc"), false, true, true);
		assertThrows(CsvFormatException.class, reader::nextRecord);
	}

	@Test
	public void testQuotedEmptyFields() throws IOException, CsvFormatException
	{
		ArrayList<String[]> expected = getRecordsEmptyFields();

		CsvReader reader = new CsvReader(getCsvQuotedEmptyFields());
		assertArrayEquals(expected.remove(0), reader.header);
		assertRecordsEquals(expected, reader);
	}

	// CSV test data

	public static ArrayList<String[]> getRecordsDefault()
	{
		ArrayList<String[]> expected = new ArrayList<>();
		expected.add(new String[]{"h1", "h2"});
		expected.add(new String[]{"f11", "f12"});
		expected.add(new String[]{"f21", "f22"});
		return expected;
	}

	public static ArrayList<String[]> getRecordsEmptyFields()
	{
		ArrayList<String[]> expected = new ArrayList<>();
		expected.add(new String[]{"h1", "", "h3"});
		expected.add(new String[]{"", "f12", "f13"});
		expected.add(new String[]{"f21", "", "f23"});
		expected.add(new String[]{"f31", "f32", ""});
		return expected;
	}

	public static ArrayList<String[]> getRecordsComma()
	{
		ArrayList<String[]> expected = new ArrayList<>();
		expected.add(new String[]{"h1", "h2"});
		expected.add(new String[]{"f1,1", "f12"});
		expected.add(new String[]{"f21", "f2,2"});
		return expected;
	}

	public static ArrayList<String[]> getRecordsCrLf()
	{
		ArrayList<String[]> expected = new ArrayList<>();
		expected.add(new String[]{"h1", "h2"});
		expected.add(new String[]{"f1\r\n1", "f12"});
		expected.add(new String[]{"f21", "f2\r\n2"});
		return expected;
	}

	public static BasicReader getCsvDefault()
	{
		return getBasicReader("h1,h2\r\nf11,f12\r\nf21,f22");
	}

	public static BasicReader getCsvEmpty()
	{
		return getBasicReader("");
	}

	public static BasicReader getCsvEmptyFields()
	{
		return getBasicReader("h1,,h3\r\n,f12,f13\r\nf21,,f23\r\nf31,f32,");
	}

	public static BasicReader getCsvComma()
	{
		return getBasicReader("h1,h2\r\n\"f1,1\",f12\r\nf21,\"f2,2\"");
	}

	public static BasicReader getCsvCrLf()
	{
		return getBasicReader("h1,h2\r\n\"f1\r\n1\",f12\r\nf21,\"f2\r\n2\"");
	}

	public static BasicReader getCsvQuotedFields()
	{
		return getBasicReader("\"h1\",\"h2\"\r\n\"f11\",\"f12\"\r\n\"f21\",\"f22\"");
	}

	public static BasicReader getCsvQuotedEmptyFields()
	{
		return getBasicReader("\"h1\",\"\",\"h3\"\r\n\"\",\"f12\",\"f13\"\r\n\"f21\",\"\",\"f23\"\r\n\"f31\",\"f32\",\"\"");
	}

	public static BasicReader getCsvEmptyLastLine()
	{
		return getBasicReader("h1,h2\r\nf11,f12\r\nf21,f22\r\n");
	}

	public static BasicReader getCsvNoRecords()
	{
		return getBasicReader("h1,h2\r\n");
	}

	public static BasicReader getCsvDiffNumFields()
	{
		return getBasicReader("h1,h2\r\nf11,f12\r\nf21\r\n");
	}

	public static BasicReader getCsvSameNames()
	{
		return getBasicReader("h1,h1\r\nf11,f12\r\nf21\r\n");
	}

	public static BasicReader.Wrapper getBasicReader(String csv)
	{
		return BasicReader.wrap(new StringReader(csv));
	}

	// Assert methods

	public static void assertRecordsEquals(Iterable<String[]> expected, Iterable<String[]> actual)
	{
		Iterator<String[]> expectedIterator = expected.iterator();
		Iterator<String[]> actualIterator = actual.iterator();
		while (expectedIterator.hasNext() && actualIterator.hasNext()) {
			assertArrayEquals(expectedIterator.next(), actualIterator.next());
		}
		assertFalse(expectedIterator.hasNext());
		assertFalse(actualIterator.hasNext());
	}
}