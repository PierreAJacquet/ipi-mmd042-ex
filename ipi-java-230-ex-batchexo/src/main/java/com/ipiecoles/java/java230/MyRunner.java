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

        try {
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        } catch (IOException e){
            logger.error("Problème dans l'ouverture du fichier" + fileName);
            return new ArrayList<>();
        }

        List<String> ligne = stream.collect(Collectors.toList());
        logger.info(ligne.size() + " lignes lues");
        for(int i = 0; i < ligne.size(); i++){
            try {
                processLine(ligne.get(i));
            } catch (BatchException e) {
               logger.error("Ligne " + (i+1) + " : "  + e.getMessage());
            }
        }

        employeRepository.save(employes);
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
            case "T":
                processTechnicien(ligne);
                break;

            case "M":
                processManager(ligne);
                break;

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

        if (splitByElement.size() == NB_CHAMPS_COMMERCIAL) {

            infosEmploye(commercial, ligneCommercial, splitByElement);

            try {
                commercial.setCaAnnuel(Double.parseDouble(splitByElement.get(5)));
            } catch (Exception e) {
                throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + splitByElement.get(5) + " => " + ligneCommercial);
            }

            try {
                commercial.setPerformance(Integer.parseInt(splitByElement.get(6)));
            } catch (Exception e) {
                throw new BatchException("La performance du commercial est incorrecte : " + splitByElement.get(6) + " => " + ligneCommercial);
            }

        } else {
            throw new BatchException("La ligne commercial ne contient pas 7 éléments mais " + splitByElement.size() + " => " + ligneCommercial);
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
        if (splitByElement.size() == NB_CHAMPS_MANAGER) {

         infosEmploye(manager, ligneManager, splitByElement);

        }else {
            throw new BatchException("La ligne manager ne contient pas 5 éléments mais " + splitByElement.size() + " => " + ligneManager);
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

        if (splitByElement.size() == NB_CHAMPS_TECHNICIEN) {

            try {
                technicien.setGrade(Integer.parseInt(splitByElement.get(5)));
            } catch (TechnicienException e) {
                throw new BatchException("Le grade doit être compris entre 1 et 5 : " + splitByElement.get(5));
            } catch (Exception e) {
                throw new BatchException(splitByElement.get(5) + " n'est pas un nombre valide pour un salaire => " + ligneTechnicien);
            }

            infosEmploye(technicien, ligneTechnicien, splitByElement);

            if ((splitByElement.get(6)).matches(REGEX_MATRICULE_MANAGER)){
                if(managerRepository.findByMatricule(splitByElement.get(6)) != null ) {
                    technicien.setManager(managerRepository.findByMatricule(splitByElement.get(6)));
                } else {
                    throw new BatchException("Le manager de matricule " + splitByElement.get(6) + " n'a pas été trouvé dans le fichier ou en base de données => " + ligneTechnicien);
                }


                /*for(int i = 0; i < employes.size() ; i++){
                    if()
                }*/

            } else {
                throw new BatchException("La châine " + splitByElement.get(6) + " ne respecte pas l'expression régulière ^M[0-9]{5}$ => " + ligneTechnicien);
            }


        } else {
            throw new BatchException("La ligne technicien ne contient pas 7 éléments mais " + splitByElement.size() + " => " + ligneTechnicien);
        }
    }

    private LocalDate stringToDate(String dateString) {
        LocalDate date = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(dateString);
        return date;
    }

    private Employe infosEmploye (Employe employe, String ligneEmploye, List<String> splitByElement ) throws BatchException{

        if ((splitByElement.get(0)).matches(REGEX_MATRICULE)) {
            employe.setMatricule(splitByElement.get(0));
        } else {
            throw new BatchException("La châine " + splitByElement.get(0) + " ne respecte pas l'expression régulière ^[MTC][0-9]{5}$ => " + ligneEmploye);
        }

        if ((splitByElement.get(1)).matches(REGEX_NOM)){
            employe.setNom(splitByElement.get(1));
        } else {
            throw new BatchException("Le nom " + splitByElement.get(1) + " n'est pas conforme => " + ligneEmploye);
        }

        if ((splitByElement.get(2)).matches(REGEX_PRENOM)){
            employe.setPrenom(splitByElement.get(2));
        } else {
            throw new BatchException("Le nom " + splitByElement.get(2) + " n'est pas conforme => " + ligneEmploye);
        }

        try {
            employe.setDateEmbauche(stringToDate(splitByElement.get(3)));
        } catch (Exception e) {
            throw new BatchException(splitByElement.get(3) + " ne respecte pas le format de date dd/MM/yyyy => " + ligneEmploye);
        }

        try {
            employe.setSalaire(Double.parseDouble(splitByElement.get(4)));
        } catch (Exception e){
            throw new BatchException(splitByElement.get(4) + " n'est pas un nombre valide pour un salaire => " + ligneEmploye);
        }

        return employe;
    }

}
