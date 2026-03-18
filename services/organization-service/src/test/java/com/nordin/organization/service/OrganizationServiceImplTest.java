package com.nordin.organization.service;

import com.nordin.organization.dto.DepartmentResponse;
import com.nordin.organization.dto.EmployeeResponse;
import com.nordin.organization.dto.OrganizationRequest;
import com.nordin.organization.dto.OrganizationResponse;
import com.nordin.organization.exception.OrganizationNotFoundException;
import com.nordin.organization.mapper.OrganizationMapper;
import com.nordin.organization.model.Organization;
import com.nordin.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrganizationService — Tests unitarios")
class OrganizationServiceImplTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationMapper organizationMapper;
    @Mock private DepartmentResilienceClient departmentResilienceClient;

    @InjectMocks private OrganizationServiceImpl organizationService;

    private UUID organizationId;
    private Organization organization;
    private OrganizationRequest request;
    private DepartmentResponse department;
    private OrganizationResponse responseWithDepts;
    private OrganizationResponse responseWithFallback;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        organization = Organization.builder()
                .id(organizationId).name("Nordin Corp").address("Calle Principal 123").build();

        request = new OrganizationRequest("Nordin Corp", "Calle Principal 123");

        EmployeeResponse employee = new EmployeeResponse(
                UUID.randomUUID(), "Juan Pérez", "juan@nordin.com", departmentId);

        department = new DepartmentResponse(
                departmentId, "Ingeniería", organizationId, List.of(employee), null);

        responseWithDepts = new OrganizationResponse(
                organizationId, "Nordin Corp", "Calle Principal 123", List.of(department), null);

        responseWithFallback = new OrganizationResponse(
                organizationId, "Nordin Corp", "Calle Principal 123", Collections.emptyList(),
                "⚠️ Información parcial: servicio de departamentos no disponible temporalmente");
    }

    @Nested @DisplayName("createOrganization")
    class CreateOrganization {

        @Test @DisplayName("debe crear y retornar la organización correctamente")
        void shouldCreateOrganizationSuccessfully() {
            when(organizationMapper.toEntity(request)).thenReturn(organization);
            when(organizationRepository.save(organization)).thenReturn(organization);
            when(organizationMapper.toResponse(organization, Collections.emptyList(), null))
                    .thenReturn(responseWithDepts);

            OrganizationResponse result = organizationService.createOrganization(request);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(organizationId);
            verify(organizationRepository).save(any(Organization.class));
        }
    }

    @Nested @DisplayName("getOrganizationById")
    class GetOrganizationById {

        @Test @DisplayName("debe retornar organización con departments")
        void shouldReturnOrganizationWithDepartments() {
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
            when(departmentResilienceClient.getDepartmentsWithResilience(organizationId))
                    .thenReturn(List.of(department));
            when(organizationMapper.toResponse(organization, List.of(department), null))
                    .thenReturn(responseWithDepts);

            OrganizationResponse result = organizationService.getOrganizationById(organizationId);

            assertThat(result).isNotNull();
            assertThat(result.departments()).hasSize(1);
        }

        @Test @DisplayName("debe retornar message de degradación cuando falla department-service")
        void shouldReturnFallbackMessage() {
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
            when(departmentResilienceClient.getDepartmentsWithResilience(organizationId))
                    .thenReturn(Collections.emptyList());
            when(organizationMapper.toResponse(eq(organization), eq(Collections.emptyList()), any()))
                    .thenReturn(responseWithFallback);

            OrganizationResponse result = organizationService.getOrganizationById(organizationId);

            assertThat(result.departments()).isEmpty();
            assertThat(result.message()).contains("⚠️");
        }

        @Test @DisplayName("debe lanzar OrganizationNotFoundException cuando no existe")
        void shouldThrowExceptionWhenNotFound() {
            when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.getOrganizationById(organizationId))
                    .isInstanceOf(OrganizationNotFoundException.class);

            verify(departmentResilienceClient, never()).getDepartmentsWithResilience(any());
        }
    }

    @Nested @DisplayName("getAllOrganizations")
    class GetAllOrganizations {

        @Test @DisplayName("debe retornar todas las organizaciones con departments")
        void shouldReturnAllOrganizations() {
            when(organizationRepository.findAll()).thenReturn(List.of(organization));
            when(departmentResilienceClient.getDepartmentsWithResilience(organizationId))
                    .thenReturn(List.of(department));
            when(organizationMapper.toResponse(organization, List.of(department), null))
                    .thenReturn(responseWithDepts);

            List<OrganizationResponse> result = organizationService.getAllOrganizations();

            assertThat(result).hasSize(1);
        }

        @Test @DisplayName("debe retornar lista vacía cuando no hay organizaciones")
        void shouldReturnEmptyList() {
            when(organizationRepository.findAll()).thenReturn(Collections.emptyList());

            List<OrganizationResponse> result = organizationService.getAllOrganizations();

            assertThat(result).isEmpty();
            verify(departmentResilienceClient, never()).getDepartmentsWithResilience(any());
        }
    }
}
