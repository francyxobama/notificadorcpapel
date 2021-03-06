package gob.pe.minam.notificadorcpapel.processor;

import gob.pe.minam.notificadorcpapel.model.*;
import gob.pe.minam.notificadorcpapel.service.IConsultaService;
import gob.pe.minam.notificadorcpapel.util.Constante;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

public class NotRechazadoItemProcess implements ItemProcessor<Expediente, Registro> {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    Environment environment;
    @Autowired
    private IConsultaService serviceConsultas;
    @Override
    public Registro process(Expediente expediente) throws Exception {

        System.out.println("NotRechazadoItemProcess:::::"+expediente.getIdExpediente());
        Consulta consulta = serviceConsultas.getConsulta();
        String sql = consulta.getSqlBuscarCiudadano();
        Ciudadano ciudadano = null;
        try {
            ciudadano = jdbcTemplate.queryForObject( sql, new Object[] { expediente.getIdExpediente() }, new CiudadanoRowMapper());
            if(Optional.ofNullable(ciudadano).isPresent()) {
                final String uri = environment.getProperty("servicios.rest.correo.minam");
                RestTemplate restTemplate = new RestTemplate();
                String cuerpo = "Estimado  administrado, Su expediente con Nro \n" +
                        expediente.getNumero() + "\n" +
                        "ha sido rechazado, sirvase revisar su casilla electronica del Sistema Cero Papel del MINAM\n" +
                        "\n" +
                        "Ministerio del Ambiente - MINAM\n" +
                        "Av. Antonio Miroquesada 425, Magdalena del Mar, Lima - Perú.  ";
                System.out.println(expediente.getIdExpediente());
                HttpEntity<MensajeCorreo> request = new HttpEntity<>(MensajeCorreo
                        .builder()
                        .hacia(ciudadano.getTxtCorreoElectronico())
                        .contenido(cuerpo)
                        .build());
                restTemplate.postForObject(uri, request, String.class);
            }
        } catch (Exception e) {
            e.getMessage();
        }


        return (Optional.ofNullable(ciudadano).isPresent())?Registro
                .builder()
                .idRegistro(ciudadano.getIdRegistro())
                .codEestaTrami(Constante.TRAMITE_RECHAZADO)
                .build():null;

    }
}
