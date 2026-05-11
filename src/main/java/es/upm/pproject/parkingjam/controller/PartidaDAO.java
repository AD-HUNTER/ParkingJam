package es.upm.pproject.parkingjam.controller;

import es.upm.pproject.parkingjam.model.Partida;
import es.upm.pproject.parkingjam.exceptions.PartidaDAOException;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileReader;

/*
 POr si acaso, este el codigo es originalmente del repositorio de ejemplo de gitlab de clase.
 Luego cambiare nombres de lo de la bookstore de ejemplo y tal
 */
public class PartidaDAO {

    private static final Logger log = Logger.getLogger(PartidaDAO.class);

    private static final String PARTIDA_XML = "." + File.separator +
            "src" + File.separator + "main" + File.separator + "resources" + File.separator + "partida" + File.separator +
            "partida.xml";

    private final JAXBContext context;

    public PartidaDAO() throws PartidaDAOException {
        try {
            context = JAXBContext.newInstance(Partida.class);
        } catch (JAXBException e) {
            throw new PartidaDAOException("Error en PartidaDAO: ", e);
        }
    }


    public void salvarPartida (Partida bookstore) throws PartidaDAOException {


        Marshaller m;
        try {
            m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            // Write to System.out (sonarqube ahora logger)
            log.info("Contenido partida: " + bookstore.toString());

            // Write to File
            m.marshal(bookstore, new File(PARTIDA_XML));
            log.info("Se ha guardado la partida en " + PARTIDA_XML);
        } catch (JAXBException e) {
            log.error("No se ha podido guardar la partida en " + PARTIDA_XML);
            throw new PartidaDAOException("Error salvar: ", e);
        }
    }

    public Partida load () throws PartidaDAOException  {
        try {

            Unmarshaller um = context.createUnmarshaller();
            Partida partida = (Partida) um.unmarshal(new FileReader(PARTIDA_XML));
            log.info("Cargando partida desde " + PARTIDA_XML);
            return partida;
        } catch (Exception e) {
            log.error("No se ha podido cargar la partida desde " + PARTIDA_XML);
            throw new PartidaDAOException("Error load: ", e);
        }
    }


}
