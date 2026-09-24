package com.pruebatecnica.solicitudes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pruebatecnica.solicitudes.config.PaginationProperties;
import com.pruebatecnica.solicitudes.dto.PageResponse;
import com.pruebatecnica.solicitudes.dto.SolicitudRequest;
import com.pruebatecnica.solicitudes.dto.SolicitudResponse;
import com.pruebatecnica.solicitudes.entity.Medicamento;
import com.pruebatecnica.solicitudes.entity.Solicitud;
import com.pruebatecnica.solicitudes.exception.MedicamentoNoDisponibleException;
import com.pruebatecnica.solicitudes.exception.RequestValidationException;
import com.pruebatecnica.solicitudes.repository.MedicamentoRepository;
import com.pruebatecnica.solicitudes.repository.SolicitudRepository;
import com.pruebatecnica.solicitudes.validator.SolicitudNoPosValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    private static final long USUARIO_ID = 7L;

    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private MedicamentoRepository medicamentoRepository;

    private SolicitudService service;

    @BeforeEach
    void setUp() {
        service = new SolicitudService(solicitudRepository, medicamentoRepository, new SolicitudNoPosValidator(),
                new PageRequestFactory(new PaginationProperties(10, 50)));
    }

    @Test
    void crear_medicamentoPos_guardaSinDatosAdicionalesAunqueLleguen() {
        Medicamento pos = medicamento(1L, "Acetaminofén", true);
        when(medicamentoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(pos));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.crear(USUARIO_ID, new SolicitudRequest(1L, "ORD-1", "Calle 1", "3001234567", "a@b.co"));

        ArgumentCaptor<Solicitud> captor = ArgumentCaptor.forClass(Solicitud.class);
        verify(solicitudRepository).save(captor.capture());
        Solicitud guardada = captor.getValue();
        assertThat(guardada.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(guardada.getNumeroOrden()).isNull();
        assertThat(guardada.getDireccion()).isNull();
        assertThat(guardada.getTelefono()).isNull();
        assertThat(guardada.getCorreoContacto()).isNull();
    }

    @Test
    void crear_medicamentoNoPosCompleto_guardaLosCuatroDatos() {
        Medicamento noPos = medicamento(2L, "Adalimumab", false);
        when(medicamentoRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(noPos));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudResponse response = service.crear(USUARIO_ID,
                new SolicitudRequest(2L, "ORD-9", "Calle 9 # 8-7", "+573001234567", "ana@correo.com"));

        assertThat(response.numeroOrden()).isEqualTo("ORD-9");
        assertThat(response.direccion()).isEqualTo("Calle 9 # 8-7");
        assertThat(response.telefono()).isEqualTo("+573001234567");
        assertThat(response.correoContacto()).isEqualTo("ana@correo.com");
        assertThat(response.medicamento().esPos()).isFalse();
    }

    @Test
    void crear_medicamentoNoPosIncompleto_lanzaValidacionYNoGuarda() {
        Medicamento noPos = medicamento(2L, "Adalimumab", false);
        when(medicamentoRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(noPos));

        assertThatThrownBy(() -> service.crear(USUARIO_ID, new SolicitudRequest(2L, "ORD-9", null, null, null)))
                .isInstanceOfSatisfying(RequestValidationException.class, ex ->
                        assertThat(ex.getErrors()).containsOnlyKeys("direccion", "telefono", "correoContacto"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void crear_medicamentoInexistenteOInactivo_lanzaNoDisponible() {
        when(medicamentoRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(USUARIO_ID, new SolicitudRequest(99L, null, null, null, null)))
                .isInstanceOf(MedicamentoNoDisponibleException.class);
    }

    @Test
    void listar_filtraPorUsuarioYMapeaLaPagina() {
        Solicitud solicitud = Solicitud.paraMedicamentoPos(USUARIO_ID, medicamento(1L, "Acetaminofén", true));
        ReflectionTestUtils.setField(solicitud, "id", 100L);
        Pageable pageable = PageRequest.of(1, 5);
        when(solicitudRepository.findByUsuarioId(any(), any()))
                .thenReturn(new PageImpl<>(List.of(solicitud), pageable, 6));

        PageResponse<SolicitudResponse> response = service.listar(USUARIO_ID, 1, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudRepository).findByUsuarioId(any(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(response.content()).extracting(SolicitudResponse::id).containsExactly(100L);
        assertThat(response.totalElements()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.page()).isEqualTo(1);
    }

    private static Medicamento medicamento(Long id, String nombre, boolean esPos) {
        Medicamento medicamento = new Medicamento(nombre, esPos, true);
        ReflectionTestUtils.setField(medicamento, "id", id);
        return medicamento;
    }
}
