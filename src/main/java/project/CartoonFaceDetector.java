package project;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.datavec.api.records.metadata.RecordMetaDataImageURI;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.objdetect.ObjectDetectionRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.objdetect.Yolo2OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.layers.objdetect.DetectedObject;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.common.io.ClassPathResource;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.learning.config.Adam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.opencv.core.CvType.CV_8U;
import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_DUPLEX;

public class CartoonFaceDetector {

    private static final Logger log = LoggerFactory.getLogger(CartoonFaceDetector.class);
    private static ComputationGraph model;
    private static int seed = 1234;
    private static Random rng = new Random(seed);

    // parameters matching the pretrained TinyYOLO model
    private static int width = 416;
    private static int height = 416;
    private static int nChannels = 3;
    private static int gridWidth = 13;
    private static int gridHeight = 13;
    private static int batchSize = 2;

    // parameters for the Yolo2OutputLayer
    private static int nBoxes = 5;
    private static double lambdaNoObj = 0.5;
    private static double lambdaCoord = 1.0;
    private static double[][] priorBoxes = {{2, 5}, {2.5, 6}, {3, 7}, {3.5, 8}, {4, 9}};
    private static double detectionThreshold = 0.1;

    // parameters for training phase
    private static List<String> labels;
    //private static String modelFilename = new File(".").getAbsolutePath() + "/generated-models/TinyYOLO_TLDetectorActors.zip";
    private static double learningRate = 1e-4;
    private static int nClasses = 2;


    public static void main(String[] args) throws Exception{

        // Directory for training and testing datasets
        File trainDir = new ClassPathResource("project/personai_icartoonface_dettrain/icartoonface_dettrain").getFile();
        File testDir = new ClassPathResource("project/personai_icartoonface_detval").getFile();
        String traincsvpath = "D:/java/TrainingLabs-main/TrainingLabs/dl4j-cv-labs/src/main/resources/project/personai_icartoonface_dettrain_anno_updatedv1.0/personai_icartoonface_dettrain_anno_updatedv1.0.csv";
        String testcsvpath = "D:/java/TrainingLabs-main/TrainingLabs/dl4j-cv-labs/src/main/resources/project/personai_icartoonface_detval.csv";

        log.info("Load data...");
        FileSplit trainData = new FileSplit(trainDir, NativeImageLoader.ALLOWED_FORMATS,rng);
        FileSplit testData = new FileSplit(testDir, NativeImageLoader.ALLOWED_FORMATS,rng);

        ObjectDetectionRecordReader recordReaderTrain = new ObjectDetectionRecordReader(
                height, width, nChannels, gridHeight, gridWidth,
                new LabelImgCsv(trainDir, traincsvpath)
        );
        recordReaderTrain.initialize(trainData);
        ObjectDetectionRecordReader recordReaderTest = new ObjectDetectionRecordReader(
                height, width, nChannels, gridHeight, gridWidth,
                new LabelImgCsv(testDir, testcsvpath)
        );
        recordReaderTest.initialize(testData);

        // Data set iterator
        RecordReaderDataSetIterator train = new RecordReaderDataSetIterator(
                recordReaderTrain, batchSize, 1, 1, true
        );
        train.setPreProcessor(new ImagePreProcessingScaler(0,1));
        RecordReaderDataSetIterator test = new RecordReaderDataSetIterator(
                recordReaderTest, 1,1,1,true);
        test.setPreProcessor(new ImagePreProcessingScaler(0,1));

        // Print Labels
        labels = train.getLabels();
        System.out.println(Arrays.toString(labels.toArray()));

        // model







    }





    private static void OfflineValidationWithTestDataset(RecordReaderDataSetIterator test) throws InterruptedException {

        // visualize results on the test set
        NativeImageLoader imageLoader = new NativeImageLoader();
        CanvasFrame frame = new CanvasFrame("Validate Test Dataset");
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        org.deeplearning4j.nn.layers.objdetect.Yolo2OutputLayer yout = (org.deeplearning4j.nn.layers.objdetect.Yolo2OutputLayer)model.getOutputLayer(0);

        test.setCollectMetaData(true);
        while (test.hasNext() && frame.isVisible()) {
            org.nd4j.linalg.dataset.DataSet ds = test.next();
            RecordMetaDataImageURI metadata = (RecordMetaDataImageURI)ds.getExampleMetaData().get(0);
            INDArray features = ds.getFeatures();
            INDArray results = model.outputSingle(features);
            List<DetectedObject> objs = yout.getPredictedObjects(results, detectionThreshold);
            List<DetectedObject> objects = NonMaxSuppression.getObjects(objs);

            File file = new File(metadata.getURI());
            log.info(file.getName() + ": " + objects);

            Mat mat = imageLoader.asMat(features);
            Mat convertedMat = new Mat();
            mat.convertTo(convertedMat, CV_8U, 255, 0);
            int w = metadata.getOrigW() * 2;
            int h = metadata.getOrigH() * 2;
            Mat image = new Mat();
            resize(convertedMat, image, new Size(w, h));
            drawBoxes(objects, w, h, image);
            frame.setTitle(new File(metadata.getURI()).getName() + " - Validate Test Dataset");
            frame.setCanvasSize(w, h);
            frame.showImage(converter.convert(image));
            frame.waitKey();
        }
        frame.dispose();
    }

    private static ComputationGraph getNewComputationGraph(ComputationGraph pretrained, INDArray priors, FineTuneConfiguration fineTuneConf) {
        ComputationGraph _ComputationGraph = new TransferLearning.GraphBuilder(pretrained)
                .fineTuneConfiguration(fineTuneConf)
                .removeVertexKeepConnections("conv2d_9")
                .removeVertexKeepConnections("outputs")
                .addLayer("convolution2d_9",
                        new ConvolutionLayer.Builder(1, 1)
                                .nIn(1024)
                                .nOut(nBoxes * (5 + nClasses))
                                .stride(1, 1)
                                .convolutionMode(ConvolutionMode.Same)
                                .weightInit(WeightInit.XAVIER)
                                .activation(Activation.IDENTITY)
                                .build(),
                        "leaky_re_lu_8")
                .addLayer("outputs",
                        new Yolo2OutputLayer.Builder()
                                .lambdaNoObj(lambdaNoObj)
                                .lambdaCoord(lambdaCoord)
                                .boundingBoxPriors(priors.castTo(DataType.FLOAT))
                                .build(),
                        "convolution2d_9")
                .setOutputs("outputs")
                .build();

        return _ComputationGraph;
    }

    private static FineTuneConfiguration getFineTuneConfiguration() {

        FineTuneConfiguration _FineTuneConfiguration = new FineTuneConfiguration.Builder()
                .seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
                .gradientNormalizationThreshold(1.0)
                .updater(new Adam.Builder().learningRate(learningRate).build())
                .l2(0.00001)
                .activation(Activation.IDENTITY)
                .trainingWorkspaceMode(WorkspaceMode.ENABLED)
                .inferenceWorkspaceMode(WorkspaceMode.ENABLED)
                .build();

        return _FineTuneConfiguration;
    }

    private static void drawBoxes(List<DetectedObject> objects, int w, int h, Mat image) {
        for (DetectedObject obj : objects) {
            double[] xy1 = obj.getTopLeftXY();
            double[] xy2 = obj.getBottomRightXY();
            String label = labels.get(obj.getPredictedClass());
            double proba = obj.getConfidence();

            int x1 = (int) Math.round(w * xy1[0] / gridWidth);
            int y1 = (int) Math.round(h * xy1[1] / gridHeight);
            int x2 = (int) Math.round(w * xy2[0] / gridWidth);
            int y2 = (int) Math.round(h * xy2[1] / gridHeight);
            rectangle(image, new Point(x1, y1), new Point(x2, y2), Scalar.RED);
            putText(
                    image,
                    label+ " - " + String.format("%.2f", proba*100) + "%",
                    new Point((x1+2) , (y1+y2)/2),
                    FONT_HERSHEY_DUPLEX,
                    0.5,
                    Scalar.RED
            );
        }
    }
}

