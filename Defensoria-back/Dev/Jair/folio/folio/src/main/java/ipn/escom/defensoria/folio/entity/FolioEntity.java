package ipn.escom.defensoria.folio.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name= "folios")
@Data
@NoArgsConstructor

    public class FolioEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(unique = true, nullable = false)
        private String codigoFolio;
        private LocalDateTime fechaCreacion;

        //Datos de la queja 
        private String asunto;
        private String fechaHechos;
        private String descripcion;
        private String unidad;  //esto lo usaremos para el catalogo

        //Datos del quejoso
        private String nombre;
        private String primerApellido;
        private String segundoApellido;
        private String correo;
        private String fechaNacimiento;

        //Datos que usaremos para identificar el usuario
        private String boleta;

        //Datos en caso de ser menor de edad
        private String nombreTutor;
        private String primerApellidoTutor;
        private String segundoApellidoTutor;
        private String parentesco;
        private String correoTutor;
        private String telefonoTutor;
        
        // Dentro de FolioEntity.java
        @OneToMany(mappedBy = "folio", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<EvidenciaEntity> evidencias = new ArrayList<>();
        

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCodigoFolio() {
            return codigoFolio;
        }

        public void setCodigoFolio(String codigoFolio) {
            this.codigoFolio = codigoFolio;
        }

        public LocalDateTime getFechaCreacion() {
            return fechaCreacion;
        }

        public void setFechaCreacion(LocalDateTime fechaCreacion) {
            this.fechaCreacion = fechaCreacion;
        }

        public String getAsunto() {
            return asunto;
        }

        public void setAsunto(String asunto) {
            this.asunto = asunto;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getUnidad() {
            return unidad;
        }

        public void setUnidad(String unidad) {
            this.unidad = unidad;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getPrimerApellido() {
            return primerApellido;
        }

        public void setPrimerApellido(String primerApellido) {
            this.primerApellido = primerApellido;
        }

        public String getSegundoApellido() {
            return segundoApellido;
        }

        public void setSegundoApellido(String segundoApellido) {
            this.segundoApellido = segundoApellido;
        }

        public String getCorreo() {
            return correo;
        }

        public void setCorreo(String correo) {
            this.correo = correo;
        }

        public String getFechaHechos() {
            return fechaHechos;
        }

        public void setFechaHechos(String fechaHechos) {
            this.fechaHechos = fechaHechos;
        }

        public String getFechaNacimiento() {
            return fechaNacimiento;
        }

        public void setFechaNacimiento(String fechaNacimiento) {
            this.fechaNacimiento = fechaNacimiento;
        }



        public String getBoleta() {
            return boleta;
        }

        public void setBoleta(String boleta) {
            this.boleta = boleta;
        }

        public String getNombreTutor() {
            return nombreTutor;
        }

        public void setNombreTutor(String nombreTutor) {
            this.nombreTutor = nombreTutor;
        }

        public String getPrimerApellidoTutor() {
            return primerApellidoTutor;
        }

        public void setPrimerApellidoTutor(String primerApellidoTutor) {
            this.primerApellidoTutor = primerApellidoTutor;
        }

        public String getSegundoApellidoTutor() {
            return segundoApellidoTutor;
        }

        public void setSegundoApellidoTutor(String segundoApellidoTutor) {
            this.segundoApellidoTutor = segundoApellidoTutor;
        }

        public String getParentesco() {
            return parentesco;
        }

        public void setParentesco(String parentesco) {
            this.parentesco = parentesco;
        }

        public String getCorreoTutor() {
            return correoTutor;
        }

        public void setCorreoTutor(String correoTutor) {
            this.correoTutor = correoTutor;
        }

        public String getTelefonoTutor() {
            return telefonoTutor;
        }

        public void setTelefonoTutor(String telefonoTutor) {
            this.telefonoTutor = telefonoTutor;
        }

        public FolioEntity(){}
    }