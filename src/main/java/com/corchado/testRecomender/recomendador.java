/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.corchado.testRecomender;

//import java.io.BufferedReader;
import java.io.File;
//import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.IRStatistics;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.eval.RecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.common.RandomUtils;

/**
 *
 * @author Corchy
 */
public class recomendador {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws org.apache.mahout.cf.taste.common.TasteException
     */
    public static void main(String[] args) throws IOException, TasteException {
        final Scanner entrada = new Scanner(System.in);

        DataModel model = new FileDataModel(new File("ml-100k/ua.base"));
        UserSimilarity similarity = null;
        int obciones;
        System.out.println("RECOMENDADOR MEDIANTE FILTRADO COLABORATIVO");
        do {
            System.out.println("Seleccione la funcion de similitud a utilizar para recomendar");
            System.out.println("marque 1 para la funcion euclidiana");
            System.out.println("marque 2 para la funcion de Pearson");
            System.out.println("marque 3 para la funcion de Log-likelihood");
            System.out.println("marque 4 para la funcion de Tanimoto");

            obciones = entrada.nextInt();
            switch (obciones) {
                case 1:
                    similarity = new EuclideanDistanceSimilarity(model);
                    break;
                case 2:
                    similarity = new PearsonCorrelationSimilarity(model);
                    break;
                case 3:
                    similarity = new LogLikelihoodSimilarity(model);
                    break;
                case 4:
                    similarity = new TanimotoCoefficientSimilarity(model);
                    break;
            }
        } while (obciones != 1 && obciones != 2 && obciones != 3 && obciones != 4);

        int queHago = 0;
        try {

            do {
                System.out.println("\n Seleccione... ");
                System.out.println("1 para realizar una recomendacion ");
                System.out.println("2 si quiere probar una recomendacion ");
                System.out.println("3 para evaluar precision y recall");
                System.out.println("4 si quiere salir");
                queHago = entrada.nextInt();
                switch (queHago) {
                    case 1:
                        System.out.println("Recomendando...");
                        Recomendar(entrada, model, similarity);
                        break;
                    case 2:
                        System.out.println("Realizando una prueba al recomendador tomado como conjunto de entrenamiento el 70% "
                                + "del dataset y el 30% como conjunto de prueba. Ademas se evalua mediante el promedio de las diferencias."
                                + "Mientras mas pequeño es este valor mejor es la recomendacion");
                        System.out.println("Si el dataset es muy grande la evaluacion puede tardar un poquito..");
                        Probar(entrada, model, similarity);
                        break;
                    case 3:
                        System.out.println("Evaluando precision y recall tomando como limite entre una recomendacion bueno o mala "
                                + "la suma entre la media y la desviaci'on estandar (threshold = μ + )...");
                        evaluarPrecicionRecall(entrada, model, similarity);
                        break;
                }
            } while (queHago != 4);
        } catch (IOException | TasteException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void Recomendar(Scanner entrada, DataModel model, final UserSimilarity similarity) throws IOException, TasteException {
        //        obtener los parametros del usuario
        int idUsuario;
        System.out.println("Entre el id de usuario");
        idUsuario = entrada.nextInt();

        System.out.println("Entre la cantidad de recomendaciones");
        int cantRecomendaciones;
        cantRecomendaciones = entrada.nextInt();
        //------------------------------------------
        //recomendador

        UserNeighborhood neighborhood = new NearestNUserNeighborhood(2, similarity, model);

        Recommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
//parametros:
//usuario al que se recomdienda
//cantidad de items a recomendar 
        List<RecommendedItem> recommendations = recommender.recommend(idUsuario, cantRecomendaciones);
       
        System.out.println("Items recomendados: ");
        for (RecommendedItem recommendation : recommendations) {
            System.out.println(recommendation);

        }

    }

    public static void Probar(final Scanner entrada, DataModel model, final UserSimilarity similarity) throws IOException, TasteException {
        RandomUtils.useTestSeed();
        RecommenderEvaluator evaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
        RecommenderBuilder builder = new RecommenderBuilder() {
            @Override
            public Recommender buildRecommender(DataModel model)
                    throws TasteException {

//                UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
                UserNeighborhood neighborhood = new NearestNUserNeighborhood(2, similarity, model);

                return new GenericUserBasedRecommender(model, neighborhood, similarity);
            }
        };

        double score = evaluator.evaluate(builder, null, model, 0.7, 1.0);
        System.out.println("evaluacion: " + score);
    }

    public static void evaluarPrecicionRecall(final Scanner entrada, DataModel model, final UserSimilarity similarity) throws IOException, TasteException {
        RandomUtils.useTestSeed();
        RecommenderIRStatsEvaluator evaluator = new GenericRecommenderIRStatsEvaluator();
        RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
            @Override
            public Recommender buildRecommender(DataModel model)
                    throws TasteException {
                UserNeighborhood neighborhood = new NearestNUserNeighborhood(2, similarity, model);
                return new GenericUserBasedRecommender(model, neighborhood, similarity);
            }
        };
        IRStatistics stats = evaluator.evaluate(recommenderBuilder, null, model, null, 2,GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD,1.0);

        System.out.println("Precision: "+stats.getPrecision());
        System.out.println("Recal: "+stats.getRecall());
    }
}
