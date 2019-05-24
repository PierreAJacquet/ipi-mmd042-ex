package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) {
        String fileName = "employes.csv";
        readFile(fileName);
        employeRepository.save(employes);
        //readFile(strings[0]);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName){
        Stream<String> stream;
        logger.info("Lecture du fichier " + fileName);

        // Ouvre le fichiez csv afin d'en lire les données //
        try {
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        } catch (IOException e){
            logger.error("Problème dans l'ouverture du fichier" + fileName);
            return new ArrayList<>();
        }

        // Initialise le message d'erreur de l'exception lorsqu'une exception est catch //
        // Sinon traitement des lignes du fichier                                       //
        List<String> ligne = stream.collect(Collectors.toList());
        logger.info(ligne.size() + " lignes lues");
        for(int i = 0; i < ligne.size(); i++){
            try {
                processLine(ligne.get(i));
            } catch (BatchException e) {
               logger.error("Ligne " + (i+1) + " : "  + e.getMessage() + " => " + ligne.get(i));
            }
        }

        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
        String firstCarac = ligne.substring(0,1);
        switch (firstCarac){

            // La lettre T correspond à un Technicien //
            case "T":
                processTechnicien(ligne);
                break;

            // La lettre M correspond à un Manager    //
            case "M":
                processManager(ligne);
                break;

            // La lettre C correspond à un Commercial //
            case "C":
                processCommercial(ligne);
                break;

            default:
                throw new BatchException("Type d'employé inconnu : " + firstCarac + " => " + ligne);
        }
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {

        List<String> splitByElement = new ArrayList<>(Arrays.asList(ligneCommercial.split(",")));
        Commercial commercial = new Commercial();

        // Vérifie que la ligne dispose du bon nombre d'élément //
        // Renvoie une Exception si False                       //
        if (splitByElement.size() == NB_CHAMPS_COMMERCIAL) {

            infosEmploye(commercial, splitByElement);

            // Effectue la conversion du Grade du type String à Double           //
            // Renvoie une erreur si : -La conversion de format echoue           //
            try {
                commercial.setCaAnnuel(Double.parseDouble(splitByElement.get(5)));
            } catch (Exception e) {
                throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + splitByElement.get(5));
            }

            // Effectue la conversion du Grade du type String à Double           //
            // Renvoie une erreur si : -La conversion de format echoue           //
            try {
                commercial.setPerformance(Integer.parseInt(splitByElement.get(6)));
            } catch (Exception e) {
                throw new BatchException("La performance du commercial est incorrecte : " + splitByElement.get(6));
            }

        } else {
            throw new BatchException("La ligne commercial ne contient pas 7 éléments mais " + splitByElement.size());
        }
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {

        Manager manager = new Manager();
        List<String> splitByElement = new ArrayList<>(Arrays.asList(ligneManager.split(",")));

        // Vérifie que la ligne dispose du bon nombre d'élément //
        // Renvoie une Exception si False                       //
        if (splitByElement.size() == NB_CHAMPS_MANAGER) {

         infosEmploye(manager, splitByElement);

        }else {
            throw new BatchException("La ligne manager ne contient pas 5 éléments mais " + splitByElement.size());
        }
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        List<String> splitByElement = new ArrayList<>(Arrays.asList(ligneTechnicien.split(",")));
        Technicien technicien = new Technicien();

        // Vérifie que la ligne dispose du bon nombre d'élément //
        // Renvoie une Exception si False                       //
        if (splitByElement.size() == NB_CHAMPS_TECHNICIEN) {

            // Effectue la conversion du Grade du type String à Integer          //
            // Renvoie une erreur si : -L'Integer n'est pas compris entre 1 et 5 //
            //                         -La conversion de format echoue           //
            try {
                technicien.setGrade(Integer.parseInt(splitByElement.get(5)));
            } catch (TechnicienException e) {
                throw new BatchException("Le grade doit être compris entre 1 et 5 : " + splitByElement.get(5));
            } catch (Exception e) {
                throw new BatchException(splitByElement.get(5) + " n'est pas un nombre valide pour un salaire");
            }

            infosEmploye(technicien, splitByElement);

            // Vérifie que le matricule du manager dont dépend le technicien, correspond à l'expression régulière //
            if ((splitByElement.get(6)).matches(REGEX_MATRICULE_MANAGER)){

                // Compare le matricule manager présent dans la ligne avec ceux présent en base //
                // Set le manager si True                                                       //
                if(managerRepository.findByMatricule(splitByElement.get(6)) != null ) {
                    technicien.setManager(managerRepository.findByMatricule(splitByElement.get(6)));
                } else {
                    throw new BatchException("Le manager de matricule " + splitByElement.get(6) + " n'a pas été trouvé dans le fichier ou en base de données");
                }

                // Compare le matricule manager présent dans la ligne avec ceux présent dans le fichiez csv //
                // Set le manager si True                                                                   //
                for(int i = 0; i < employes.size() ; i++){

                    // Compare le matricule manager présent dans la ligne avec ceux présent dans le fichiez csv //
                    // Set le manager si True                                                                   //
                    if (splitByElement.get(6).matches(employes.get(i).getMatricule())){
                        technicien.setManager((Manager) employes.get(i));
                    }

                    else {
                        throw new BatchException("Le manager de matricule " + splitByElement.get(6) + " n'a pas été trouvé dans le fichier ou en base de données");
                    }
                }

            } else {
                throw new BatchException("La châine " + splitByElement.get(6) + " ne respecte pas l'expression régulière ^M[0-9]{5}$");
            }


        } else {
            throw new BatchException("La ligne technicien ne contient pas 7 éléments mais " + splitByElement.size());
        }
    }

    /**
     * Fonction permettant de modifier le format des données pour passer d'un format String à un format Date
     * @param dateString
     * @return date
     */
    private LocalDate stringToDate(String dateString) {
        LocalDate date = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(dateString);
        return date;
    }

    /**
     * Fonction permettant de regrouper les informations communes aux employés, peut-importe qu'il soit manager, tech ou commercial
     * @param employe, instance d'employe se spécialisant ensuit en manager, tech ou commercial
     * @param splitByElement, correspondant à la fonction permettant de découper la ligne de texte en incrémentant chaque mot dans une liste
     * @return employe
     * @throws BatchException , divers exception détaillé si dessous
     */
    private Employe infosEmploye (Employe employe, List<String> splitByElement ) throws BatchException{

        // Vérifie que le matricule de l'employé correspond à l'expression régulière //
        // Set le matricule si True                                                  //
        if ((splitByElement.get(0)).matches(REGEX_MATRICULE)) {
            employe.setMatricule(splitByElement.get(0));
        } else {
            throw new BatchException("La châine " + splitByElement.get(0) + " ne respecte pas l'expression régulière ^[MTC][0-9]{5}$");
        }

        // Vérifie que le nom de l'employé correspond à l'expression régulière //
        // Set le nom si True                                                  //
        if ((splitByElement.get(1)).matches(REGEX_NOM)){
            employe.setNom(splitByElement.get(1));
        } else {
            throw new BatchException("Le nom " + splitByElement.get(1) + " n'est pas conforme");
        }

        // Vérifie que le prénom de l'employé correspond à l'expression régulière //
        // Set le prénom si True                                                  //
        if ((splitByElement.get(2)).matches(REGEX_PRENOM)){
            employe.setPrenom(splitByElement.get(2));
        } else {
            throw new BatchException("Le nom " + splitByElement.get(2) + " n'est pas conforme");
        }

        // Vérifie que la date est bien au bon format  //
        // Set la date si True                         //
        try {
            employe.setDateEmbauche(stringToDate(splitByElement.get(3)));
        } catch (Exception e) {
            throw new BatchException(splitByElement.get(3) + " ne respecte pas le format de date dd/MM/yyyy");
        }

        // Convertit le salaire du type String à Double                      //
        // Renvoie une exception si mauvais format ou problème de conversion //
        try {
            employe.setSalaire(Double.parseDouble(splitByElement.get(4)));
        } catch (Exception e){
            throw new BatchException(splitByElement.get(4) + " n'est pas un nombre valide pour un salaire");
        }

        return employe;
    }

}
