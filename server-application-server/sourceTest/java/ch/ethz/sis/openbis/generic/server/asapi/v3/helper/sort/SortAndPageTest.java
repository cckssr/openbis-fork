/*
 * Copyright ETH 2015 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.generic.server.asapi.v3.helper.sort;

import static org.testng.AssertJUnit.assertEquals;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.Tag;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.sort.SortAndPage;

public class SortAndPageTest
{
	
	@Test
    public void testFetchedFieldsScore_Sample_CodeScore()
    {
		SampleType sampleTypeA = new SampleType();
		sampleTypeA.setCode("DUMMY_CODE_A");
		
		SampleType sampleTypeB = new SampleType();
		sampleTypeB.setCode("DUMMY_CODE_B");
		
		String propertyCode = "DUMMY_PROPERTY";
		
		SampleSearchCriteria c = new SampleSearchCriteria();
		c.withOrOperator();
        c.withCode().thatEquals("S2");
        
		SampleFetchOptions fo = new SampleFetchOptions();
		fo.withType();
		fo.withProperties();
		fo.sortBy().fetchedFieldsScore();
		
        Sample sample1 = new Sample();
        sample1.setType(sampleTypeA);
        sample1.setCode("S1");
        sample1.setProperty(propertyCode, "DUMMY_S1");
        sample1.setFetchOptions(fo);

        Sample sample2 = new Sample();
        sample2.setType(sampleTypeB);
        sample2.setCode("S2");
        sample2.setProperty(propertyCode, "DUMMY_S2");
        sample2.setFetchOptions(fo);

        Sample sample3 = new Sample();
        sample3.setType(sampleTypeA);
        sample3.setCode("S3");
        sample3.setProperty(propertyCode, "DUMMY_S3");
        sample3.setFetchOptions(fo);

        List<Sample> samples = new ArrayList<Sample>();
        samples.add(sample1);
        samples.add(sample2);
        samples.add(sample3);

        Collection<Sample> results = new SortAndPage().sortAndPage(samples, c, fo);

        assertEquals(results, list(sample2, sample1, sample3));
    }
	
	@Test
    public void testFetchedFieldsScore_Sample_PropertyScore()
    {
		SampleType sampleTypeA = new SampleType();
		sampleTypeA.setCode("DUMMY_CODE_A");
		
		SampleType sampleTypeB = new SampleType();
		sampleTypeB.setCode("DUMMY_CODE_B");
		
		String propertyCode = "DUMMY_PROPERTY";
		
		SampleSearchCriteria c = new SampleSearchCriteria();
		c.withOrOperator();
        c.withProperty(propertyCode).thatEquals("DUMMY_S3");
        
		SampleFetchOptions fo = new SampleFetchOptions();
		fo.withType();
		fo.withProperties();
		fo.sortBy().fetchedFieldsScore();
		
        Sample sample1 = new Sample();
        sample1.setType(sampleTypeA);
        sample1.setCode("S1");
        sample1.setProperty(propertyCode, "DUMMY_S1");
        sample1.setFetchOptions(fo);

        Sample sample2 = new Sample();
        sample2.setType(sampleTypeB);
        sample2.setCode("S2");
        sample2.setProperty(propertyCode, "DUMMY_S2");
        sample2.setFetchOptions(fo);

        Sample sample3 = new Sample();
        sample3.setType(sampleTypeA);
        sample3.setCode("S3");
        sample3.setProperty(propertyCode, "DUMMY_S3");
        sample3.setFetchOptions(fo);

        List<Sample> samples = new ArrayList<Sample>();
        samples.add(sample1);
        samples.add(sample2);
        samples.add(sample3);

        Collection<Sample> results = new SortAndPage().sortAndPage(samples, c, fo);

        assertEquals(results, list(sample3, sample1, sample2));
    }
	
	@Test
    public void testFetchedFieldsScore_Sample_PropertyScore_MissingProperties()
    {
		SampleType sampleTypeA = new SampleType();
		sampleTypeA.setCode("DUMMY_CODE_A");
		
		SampleType sampleTypeB = new SampleType();
		sampleTypeB.setCode("DUMMY_CODE_B");
		
		String propertyCode = "DUMMY_PROPERTY";
		
		SampleSearchCriteria c = new SampleSearchCriteria();
		c.withOrOperator();
        c.withProperty(propertyCode).thatEquals("DUMMY_S3");
        
		SampleFetchOptions fo = new SampleFetchOptions();
		fo.withType();
		fo.sortBy().fetchedFieldsScore();
		
        Sample sample1 = new Sample();
        sample1.setType(sampleTypeA);
        sample1.setCode("S1");
        sample1.setFetchOptions(fo);

        Sample sample2 = new Sample();
        sample2.setType(sampleTypeB);
        sample2.setCode("S2");
        sample2.setFetchOptions(fo);

        Sample sample3 = new Sample();
        sample3.setType(sampleTypeA);
        sample3.setCode("S3");
        sample3.setFetchOptions(fo);

        List<Sample> samples = new ArrayList<Sample>();
        samples.add(sample1);
        samples.add(sample2);
        samples.add(sample3);

        Collection<Sample> results = new SortAndPage().sortAndPage(samples, c, fo);

        assertEquals(results, list(sample1, sample2, sample3));
    }
	
	@Test
    public void testFetchedFieldsScore_Sample_TypeScore()
    {
		SampleType sampleTypeA = new SampleType();
		sampleTypeA.setCode("DUMMY_CODE_A");
		
		SampleType sampleTypeB = new SampleType();
		sampleTypeB.setCode("DUMMY_CODE_B");
		
		String propertyCode = "DUMMY_PROPERTY";
		
		SampleSearchCriteria c = new SampleSearchCriteria();
		c.withOrOperator();
        c.withType().withCode().thatEquals(sampleTypeA.getCode());
        
		SampleFetchOptions fo = new SampleFetchOptions();
		fo.withType();
		fo.withProperties();
		fo.sortBy().fetchedFieldsScore();
		
        Sample sample1 = new Sample();
        sample1.setType(sampleTypeA);
        sample1.setCode("S1");
        sample1.setProperty(propertyCode, "DUMMY_S1");
        sample1.setFetchOptions(fo);

        Sample sample2 = new Sample();
        sample2.setType(sampleTypeB);
        sample2.setCode("S2");
        sample2.setProperty(propertyCode, "DUMMY_S2");
        sample2.setFetchOptions(fo);

        Sample sample3 = new Sample();
        sample3.setType(sampleTypeA);
        sample3.setCode("S3");
        sample3.setProperty(propertyCode, "DUMMY_S3");
        sample3.setFetchOptions(fo);

        List<Sample> samples = new ArrayList<Sample>();
        samples.add(sample1);
        samples.add(sample2);
        samples.add(sample3);

        Collection<Sample> results = new SortAndPage().sortAndPage(samples, c, fo);

        assertEquals(results, list(sample1, sample3, sample2));
    }
	
	@Test
    public void testFetchedFieldsScore_Sample_TypeScore_MissingType()
    {
		SampleType sampleTypeA = new SampleType();
		sampleTypeA.setCode("DUMMY_CODE_A");
		
		String propertyCode = "DUMMY_PROPERTY";
		
		SampleSearchCriteria c = new SampleSearchCriteria();
		c.withOrOperator();
        c.withType().withCode().thatEquals(sampleTypeA.getCode());
        
		SampleFetchOptions fo = new SampleFetchOptions();
		fo.withProperties();
		fo.sortBy().fetchedFieldsScore();
		
        Sample sample1 = new Sample();
        sample1.setCode("S1");
        sample1.setProperty(propertyCode, "DUMMY_S1");
        sample1.setFetchOptions(fo);

        Sample sample2 = new Sample();
        sample2.setCode("S2");
        sample2.setProperty(propertyCode, "DUMMY_S2");
        sample2.setFetchOptions(fo);

        Sample sample3 = new Sample();
        sample3.setCode("S3");
        sample3.setProperty(propertyCode, "DUMMY_S3");
        sample3.setFetchOptions(fo);

        List<Sample> samples = new ArrayList<Sample>();
        samples.add(sample1);
        samples.add(sample2);
        samples.add(sample3);

        Collection<Sample> results = new SortAndPage().sortAndPage(samples, c, fo);

        assertEquals(results, list(sample1, sample2, sample3));
    }
	
	@Test
    public void testFetchedFieldsScore_Experiment_CodeScore()
    {
		ExperimentType experimentTypeA = new ExperimentType();
		experimentTypeA.setCode("DUMMY_CODE_A");
		
		ExperimentType experimentTypeB = new ExperimentType();
		experimentTypeB.setCode("DUMMY_CODE_B");
		
		String propertyCode = "DUMMY_PROPERTY";
		
		ExperimentSearchCriteria c = new ExperimentSearchCriteria();
		c.withOrOperator();
        c.withCode().thatEquals("S2");
        
		ExperimentFetchOptions fo = new ExperimentFetchOptions();
		fo.withType();
		fo.withProperties();
		fo.sortBy().fetchedFieldsScore();
		
        Experiment experiment1 = new Experiment();
        experiment1.setType(experimentTypeA);
        experiment1.setCode("S1");
        experiment1.setProperty(propertyCode, "DUMMY_S1");
        experiment1.setFetchOptions(fo);

        Experiment experiment2 = new Experiment();
        experiment2.setType(experimentTypeB);
        experiment2.setCode("S2");
        experiment2.setProperty(propertyCode, "DUMMY_S2");
        experiment2.setFetchOptions(fo);

        Experiment experiment3 = new Experiment();
        experiment3.setType(experimentTypeA);
        experiment3.setCode("S3");
        experiment3.setProperty(propertyCode, "DUMMY_S3");
        experiment3.setFetchOptions(fo);

        List<Experiment> experiments = new ArrayList<Experiment>();
        experiments.add(experiment1);
        experiments.add(experiment2);
        experiments.add(experiment3);

        Collection<Experiment> results = new SortAndPage().sortAndPage(experiments, c, fo);

        assertEquals(results, list(experiment2, experiment1, experiment3));
    }
	
	@Test
    public void testFetchedFieldsScore_Experiment_PropertyScore()
    {
		ExperimentType experimentTypeA = new ExperimentType();
		experimentTypeA.setCode("DUMMY_CODE_A");
		
		ExperimentType experimentTypeB = new ExperimentType();
		experimentTypeB.setCode("DUMMY_CODE_B");
		
		String propertyCode = "DUMMY_PROPERTY";
		
		ExperimentSearchCriteria c = new ExperimentSearchCriteria();
		c.withOrOperator();
        c.withProperty(propertyCode).thatEquals("DUMMY_S3");
        
		ExperimentFetchOptions fo = new ExperimentFetchOptions();
		fo.withType();
		fo.withProperties();
		fo.sortBy().fetchedFieldsScore();
		
        Experiment experiment1 = new Experiment();
        experiment1.setType(experimentTypeA);
        experiment1.setCode("S1");
        experiment1.setProperty(propertyCode, "DUMMY_S1");
        experiment1.setFetchOptions(fo);

        Experiment experiment2 = new Experiment();
        experiment2.setType(experimentTypeB);
        experiment2.setCode("S2");
        experiment2.setProperty(propertyCode, "DUMMY_S2");
        experiment2.setFetchOptions(fo);

        Experiment experiment3 = new Experiment();
        experiment3.setType(experimentTypeA);
        experiment3.setCode("S3");
        experiment3.setProperty(propertyCode, "DUMMY_S3");
        experiment3.setFetchOptions(fo);

        List<Experiment> experiments = new ArrayList<Experiment>();
        experiments.add(experiment1);
        experiments.add(experiment2);
        experiments.add(experiment3);

        Collection<Experiment> results = new SortAndPage().sortAndPage(experiments, c, fo);

        assertEquals(results, list(experiment3, experiment1, experiment2));
    }
	
	@Test
    public void testFetchedFieldsScore_Experiment_PropertyScore_MissingProperties()
    {
		ExperimentType experimentTypeA = new ExperimentType();
		experimentTypeA.setCode("DUMMY_CODE_A");
		
		ExperimentType experimentTypeB = new ExperimentType();
		experimentTypeB.setCode("DUMMY_CODE_B");
		
		String propertyCode = "DUMMY_PROPERTY";
		
		ExperimentSearchCriteria c = new ExperimentSearchCriteria();
		c.withOrOperator();
        c.withProperty(propertyCode).thatEquals("DUMMY_S3");
        
		ExperimentFetchOptions fo = new ExperimentFetchOptions();
		fo.withType();
		fo.sortBy().fetchedFieldsScore();
		
        Experiment experiment1 = new Experiment();
        experiment1.setType(experimentTypeA);
        experiment1.setCode("S1");
        experiment1.setFetchOptions(fo);

        Experiment experiment2 = new Experiment();
        experiment2.setType(experimentTypeB);
        experiment2.setCode("S2");
        experiment2.setFetchOptions(fo);

        Experiment experiment3 = new Experiment();
        experiment3.setType(experimentTypeA);
        experiment3.setCode("S3");
        experiment3.setFetchOptions(fo);

        List<Experiment> experiments = new ArrayList<Experiment>();
        experiments.add(experiment1);
        experiments.add(experiment2);
        experiments.add(experiment3);

        Collection<Experiment> results = new SortAndPage().sortAndPage(experiments, c, fo);

        assertEquals(results, list(experiment1, experiment2, experiment3));
    }
	
	@Test
    public void testFetchedFieldsScore_Experiment_TypeScore()
    {
		ExperimentType experimentTypeA = new ExperimentType();
		experimentTypeA.setCode("DUMMY_CODE_A");
		
		ExperimentType experimentTypeB = new ExperimentType();
		experimentTypeB.setCode("DUMMY_CODE_B");
		
		String propertyCode = "DUMMY_PROPERTY";
		
		ExperimentSearchCriteria c = new ExperimentSearchCriteria();
		c.withOrOperator();
        c.withType().withCode().thatEquals(experimentTypeA.getCode());
        
		ExperimentFetchOptions fo = new ExperimentFetchOptions();
		fo.withType();
		fo.withProperties();
		fo.sortBy().fetchedFieldsScore();
		
        Experiment experiment1 = new Experiment();
        experiment1.setType(experimentTypeA);
        experiment1.setCode("S1");
        experiment1.setProperty(propertyCode, "DUMMY_S1");
        experiment1.setFetchOptions(fo);

        Experiment experiment2 = new Experiment();
        experiment2.setType(experimentTypeB);
        experiment2.setCode("S2");
        experiment2.setProperty(propertyCode, "DUMMY_S2");
        experiment2.setFetchOptions(fo);

        Experiment experiment3 = new Experiment();
        experiment3.setType(experimentTypeA);
        experiment3.setCode("S3");
        experiment3.setProperty(propertyCode, "DUMMY_S3");
        experiment3.setFetchOptions(fo);

        List<Experiment> experiments = new ArrayList<Experiment>();
        experiments.add(experiment1);
        experiments.add(experiment2);
        experiments.add(experiment3);

        Collection<Experiment> results = new SortAndPage().sortAndPage(experiments, c, fo);

        assertEquals(results, list(experiment1, experiment3, experiment2));
    }
	
	@Test
    public void testFetchedFieldsScore_Experiment_TypeScore_MissingType()
    {
		ExperimentType experimentTypeA = new ExperimentType();
		experimentTypeA.setCode("DUMMY_CODE_A");
		
		String propertyCode = "DUMMY_PROPERTY";
		
		ExperimentSearchCriteria c = new ExperimentSearchCriteria();
		c.withOrOperator();
        c.withType().withCode().thatEquals(experimentTypeA.getCode());
        
		ExperimentFetchOptions fo = new ExperimentFetchOptions();
		fo.withProperties();
		fo.sortBy().fetchedFieldsScore();
		
        Experiment experiment1 = new Experiment();
        experiment1.setCode("S1");
        experiment1.setProperty(propertyCode, "DUMMY_S1");
        experiment1.setFetchOptions(fo);

        Experiment experiment2 = new Experiment();
        experiment2.setCode("S2");
        experiment2.setProperty(propertyCode, "DUMMY_S2");
        experiment2.setFetchOptions(fo);

        Experiment experiment3 = new Experiment();
        experiment3.setCode("S3");
        experiment3.setProperty(propertyCode, "DUMMY_S3");
        experiment3.setFetchOptions(fo);

        List<Experiment> experiments = new ArrayList<Experiment>();
        experiments.add(experiment1);
        experiments.add(experiment2);
        experiments.add(experiment3);

        Collection<Experiment> results = new SortAndPage().sortAndPage(experiments, c, fo);

        assertEquals(results, list(experiment1, experiment2, experiment3));
    }
	
	@Test
    public void testFetchedFieldsScore_DataSet_CodeScore()
    {
        DataSetType dataSetTypeA = new DataSetType();
        dataSetTypeA.setCode("DUMMY_CODE_A");
        
        DataSetType dataSetTypeB = new DataSetType();
        dataSetTypeB.setCode("DUMMY_CODE_B");
        
        String propertyCode = "DUMMY_PROPERTY";
        
        DataSetSearchCriteria c = new DataSetSearchCriteria();
        c.withOrOperator();
        c.withCode().thatEquals("S2");
        
        DataSetFetchOptions fo = new DataSetFetchOptions();
        fo.withType();
        fo.withProperties();
        fo.sortBy().fetchedFieldsScore();
        
        DataSet dataset1 = new DataSet();
        dataset1.setType(dataSetTypeA);
        dataset1.setCode("S1");
        dataset1.setProperty(propertyCode, "DUMMY_S1");
        dataset1.setFetchOptions(fo);

        DataSet dataset2 = new DataSet();
        dataset2.setType(dataSetTypeB);
        dataset2.setCode("S2");
        dataset2.setProperty(propertyCode, "DUMMY_S2");
        dataset2.setFetchOptions(fo);

        DataSet dataset3 = new DataSet();
        dataset3.setType(dataSetTypeA);
        dataset3.setCode("S3");
        dataset3.setProperty(propertyCode, "DUMMY_S3");
        dataset3.setFetchOptions(fo);

        List<DataSet> datasets = new ArrayList<DataSet>();
        datasets.add(dataset1);
        datasets.add(dataset2);
        datasets.add(dataset3);

        Collection<DataSet> results = new SortAndPage().sortAndPage(datasets, c, fo);

        assertEquals(results, list(dataset2, dataset1, dataset3));
    }
    
    @Test
    public void testFetchedFieldsScore_DataSet_PropertyScore()
    {
        DataSetType dataSetTypeA = new DataSetType();
        dataSetTypeA.setCode("DUMMY_CODE_A");
        
        DataSetType dataSetTypeB = new DataSetType();
        dataSetTypeB.setCode("DUMMY_CODE_B");
        
        String propertyCode = "DUMMY_PROPERTY";
        
        DataSetSearchCriteria c = new DataSetSearchCriteria();
        c.withOrOperator();
        c.withProperty(propertyCode).thatEquals("DUMMY_S3");
        
        DataSetFetchOptions fo = new DataSetFetchOptions();
        fo.withType();
        fo.withProperties();
        fo.sortBy().fetchedFieldsScore();
        
        DataSet dataset1 = new DataSet();
        dataset1.setType(dataSetTypeA);
        dataset1.setCode("S1");
        dataset1.setProperty(propertyCode, "DUMMY_S1");
        dataset1.setFetchOptions(fo);

        DataSet dataset2 = new DataSet();
        dataset2.setType(dataSetTypeB);
        dataset2.setCode("S2");
        dataset2.setProperty(propertyCode, "DUMMY_S2");
        dataset2.setFetchOptions(fo);

        DataSet dataset3 = new DataSet();
        dataset3.setType(dataSetTypeA);
        dataset3.setCode("S3");
        dataset3.setProperty(propertyCode, "DUMMY_S3");
        dataset3.setFetchOptions(fo);

        List<DataSet> datasets = new ArrayList<DataSet>();
        datasets.add(dataset1);
        datasets.add(dataset2);
        datasets.add(dataset3);

        Collection<DataSet> results = new SortAndPage().sortAndPage(datasets, c, fo);

        assertEquals(results, list(dataset3, dataset1, dataset2));
    }
    
    @Test
    public void testFetchedFieldsScore_DataSet_PropertyScore_MissingProperties()
    {
        DataSetType dataSetTypeA = new DataSetType();
        dataSetTypeA.setCode("DUMMY_CODE_A");
        
        DataSetType dataSetTypeB = new DataSetType();
        dataSetTypeB.setCode("DUMMY_CODE_B");
        
        String propertyCode = "DUMMY_PROPERTY";
        
        DataSetSearchCriteria c = new DataSetSearchCriteria();
        c.withOrOperator();
        c.withProperty(propertyCode).thatEquals("DUMMY_S3");
        
        DataSetFetchOptions fo = new DataSetFetchOptions();
        fo.withType();
        fo.sortBy().fetchedFieldsScore();
        
        DataSet dataset1 = new DataSet();
        dataset1.setType(dataSetTypeA);
        dataset1.setCode("S1");
        dataset1.setFetchOptions(fo);

        DataSet dataset2 = new DataSet();
        dataset2.setType(dataSetTypeB);
        dataset2.setCode("S2");
        dataset2.setFetchOptions(fo);

        DataSet dataset3 = new DataSet();
        dataset3.setType(dataSetTypeA);
        dataset3.setCode("S3");
        dataset3.setFetchOptions(fo);

        List<DataSet> datasets = new ArrayList<DataSet>();
        datasets.add(dataset1);
        datasets.add(dataset2);
        datasets.add(dataset3);

        Collection<DataSet> results = new SortAndPage().sortAndPage(datasets, c, fo);

        assertEquals(results, list(dataset1, dataset2, dataset3));
    }
    
    @Test
    public void testFetchedFieldsScore_DataSet_TypeScore()
    {
        DataSetType dataSetTypeA = new DataSetType();
        dataSetTypeA.setCode("DUMMY_CODE_A");
        
        DataSetType dataSetTypeB = new DataSetType();
        dataSetTypeB.setCode("DUMMY_CODE_B");
        
        String propertyCode = "DUMMY_PROPERTY";
        
        DataSetSearchCriteria c = new DataSetSearchCriteria();
        c.withOrOperator();
        c.withType().withCode().thatEquals(dataSetTypeA.getCode());
        
        DataSetFetchOptions fo = new DataSetFetchOptions();
        fo.withType();
        fo.withProperties();
        fo.sortBy().fetchedFieldsScore();
        
        DataSet dataset1 = new DataSet();
        dataset1.setType(dataSetTypeA);
        dataset1.setCode("S1");
        dataset1.setProperty(propertyCode, "DUMMY_S1");
        dataset1.setFetchOptions(fo);

        DataSet dataset2 = new DataSet();
        dataset2.setType(dataSetTypeB);
        dataset2.setCode("S2");
        dataset2.setProperty(propertyCode, "DUMMY_S2");
        dataset2.setFetchOptions(fo);

        DataSet dataset3 = new DataSet();
        dataset3.setType(dataSetTypeA);
        dataset3.setCode("S3");
        dataset3.setProperty(propertyCode, "DUMMY_S3");
        dataset3.setFetchOptions(fo);

        List<DataSet> datasets = new ArrayList<DataSet>();
        datasets.add(dataset1);
        datasets.add(dataset2);
        datasets.add(dataset3);

        Collection<DataSet> results = new SortAndPage().sortAndPage(datasets, c, fo);

        assertEquals(results, list(dataset1, dataset3, dataset2));
    }
    
    @Test
    public void testFetchedFieldsScore_DataSet_TypeScore_MissingType()
    {
        DataSetType dataSetTypeA = new DataSetType();
        dataSetTypeA.setCode("DUMMY_CODE_A");
        
        String propertyCode = "DUMMY_PROPERTY";
        
        DataSetSearchCriteria c = new DataSetSearchCriteria();
        c.withOrOperator();
        c.withType().withCode().thatEquals(dataSetTypeA.getCode());
        
        DataSetFetchOptions fo = new DataSetFetchOptions();
        fo.withProperties();
        fo.sortBy().fetchedFieldsScore();
        
        DataSet dataset1 = new DataSet();
        dataset1.setCode("S1");
        dataset1.setProperty(propertyCode, "DUMMY_S1");
        dataset1.setFetchOptions(fo);

        DataSet dataset2 = new DataSet();
        dataset2.setCode("S2");
        dataset2.setProperty(propertyCode, "DUMMY_S2");
        dataset2.setFetchOptions(fo);

        DataSet dataset3 = new DataSet();
        dataset3.setCode("S3");
        dataset3.setProperty(propertyCode, "DUMMY_S3");
        dataset3.setFetchOptions(fo);

        List<DataSet> datasets = new ArrayList<DataSet>();
        datasets.add(dataset1);
        datasets.add(dataset2);
        datasets.add(dataset3);

        Collection<DataSet> results = new SortAndPage().sortAndPage(datasets, c, fo);

        assertEquals(results, list(dataset1, dataset2, dataset3));
    }


    @SuppressWarnings("unchecked")
    private <T> List<T> list(T... items)
    {
        return Arrays.asList(items);
    }

    @SuppressWarnings("unchecked")
    private <T> Set<T> set(T... items)
    {
        return new LinkedHashSet<T>(Arrays.asList(items));
    }

}
