package minimarketdemo.model.seguridades.managers;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import minimarketdemo.model.auditoria.managers.ManagerAuditoria;
import minimarketdemo.model.core.entities.SegAsignacion;
import minimarketdemo.model.core.entities.SegModulo;
import minimarketdemo.model.core.entities.SegUsuario;
import minimarketdemo.model.core.entities.ThmEmpleado;
import minimarketdemo.model.core.managers.ManagerDAO;
import minimarketdemo.model.core.utils.ModelUtil;
import minimarketdemo.model.seguridades.dtos.LoginDTO;

/**
 * Implementa la logica de manejo de usuarios y autenticacion.
 * Funcionalidades principales:
 * <ul>
 * 	<li>Verificacion de credenciales de usuario.</li>
 *  <li>Asignacion de modulos a un usuario.</li>
 * </ul>
 * @author john
 */
@Stateless
@LocalBean
public class ManagerSeguridades {
	@EJB
	private ManagerDAO mDAO;
	@EJB
	private ManagerAuditoria mAuditoria;
    /**
     * Default constructor. 
     */
    public ManagerSeguridades() {
        
    }
    /**
     * Funcion de inicializacion de datos de usuarios.
     */
    public void inicializarDemo() throws Exception {
    	mAuditoria.mostrarLog(getClass(), "inicializarDemo", "Inicializacion de bdd demo.");
    	List<SegUsuario> listaUsuarios=mDAO.findAll(SegUsuario.class);
    	int idSegUsuarioAdmin=0;
    	
    	boolean existeAdministrador=false;
    	for(SegUsuario u:listaUsuarios) {
    		mAuditoria.mostrarLog(getClass(), "inicializarDemo", "Info de usuario "+u.getCorreo()+" "+u.getIdSegUsuario());
    		//Se considera al usuario 1 como administrador: 
    		if(u.getIdSegUsuario()==1) {
    			existeAdministrador=true;
    			idSegUsuarioAdmin=1;
    			System.out.println("Ya existe un usuario administrador (con id usuario 1)");
    		}
    	}
    	
    	
    	//creacion del usuario administrador:
    	if(existeAdministrador==false) {
			SegUsuario administrador=new SegUsuario();
			administrador.setApellidos("admin");
			administrador.setClave("admin");
			administrador.setCorreo("admin@minimarketdemo.com");
			administrador.setNombres("admin");
			administrador.setCodigo("admin");
			mDAO.insertar(administrador);
			idSegUsuarioAdmin=administrador.getIdSegUsuario();
			mAuditoria.mostrarLog(getClass(), "inicializarDemo", "Usuario administrador creado (id : "+idSegUsuarioAdmin);
    	}
		//inicializacion de modulos:
		SegModulo modulo=new SegModulo();
		int idSegModuloSeguridades=0;
		int idSegModuloAuditoria=0;
		modulo.setNombreModulo("Seguridades");
		modulo.setRutaAcceso("seguridades/menu");
		mDAO.insertar(modulo);
		idSegModuloSeguridades=modulo.getIdSegModulo();
		modulo=new SegModulo();
		modulo.setNombreModulo("Auditoría");
		modulo.setRutaAcceso("auditoria/menu");
		mDAO.insertar(modulo);
		idSegModuloAuditoria=modulo.getIdSegModulo();
		mAuditoria.mostrarLog(getClass(), "inicializarDemo", "Modulos creados.");
		//asignacion de accesos:
		asignarModulo(idSegUsuarioAdmin, idSegModuloSeguridades);
		asignarModulo(idSegUsuarioAdmin, idSegModuloAuditoria);
		mAuditoria.mostrarLog(getClass(), "inicializarDemo", "Inicializacion de bdd demo terminada.");
    }
    
    /**
     * Funcion de autenticacion mediante el uso de credenciales.
     * @param idSegUsuario El codigo del usuario que desea autenticarse.
     * @param clave La contrasenia.
     * @param direccionIP La direcci�n IP V$ del cliente web.
     * @return La ruta de acceso al sistema.
     * @throws Exception
     */
    public LoginDTO login(int idSegUsuario,String clave, String direccionIP) throws Exception{
    	//crear DTO:
		LoginDTO loginDTO=new LoginDTO();
		loginDTO.setIdSegUsuario(idSegUsuario);
		loginDTO.setDireccionIP(direccionIP);
		
    	if(ModelUtil.isEmpty(clave)) {
    		mAuditoria.mostrarLog(getClass(), "login", "Debe indicar una clave "+idSegUsuario);
    		throw new Exception("Debe indicar una clave");
    	}
    	SegUsuario usuario=(SegUsuario) mDAO.findById(SegUsuario.class, idSegUsuario);
    	if(usuario==null) {
    		mAuditoria.mostrarLog(getClass(), "login", "No existe usuario "+idSegUsuario);
    		throw new Exception("Error en credenciales.");
    	}
    		
    	if(usuario.getClave().equals(clave)) {
    		mAuditoria.mostrarLog(loginDTO,getClass(), "login", "Login exitoso: "+idSegUsuario);
    		loginDTO.setCorreo(usuario.getCorreo());
    		//aqui puede realizarse el envio de correo de notificacion
    		//obtener la lista de modulos a los que tiene acceso:
    		List<SegAsignacion> listaAsignaciones=findAsignacionByUsuario(usuario.getIdSegUsuario());
    		for(SegAsignacion asig:listaAsignaciones) {
    			SegModulo modulo=asig.getSegModulo();
    			loginDTO.getListaModulos().add(modulo);
    		}
    		return loginDTO;
    	}
    	mAuditoria.mostrarLog(getClass(), "login", "No coincide la clave "+idSegUsuario);
    	throw new Exception("Error en credenciales");
    }
    
    public LoginDTO cerrarSesion(int idSegUsuario, String direccionIP) {
    	LoginDTO loginDTO=new LoginDTO();
		loginDTO.setIdSegUsuario(idSegUsuario);
		loginDTO.setDireccionIP(direccionIP);
    	mAuditoria.mostrarLog(loginDTO, getClass(), "cerrarSesion", "Cerrar sesi�nn usuario: "+idSegUsuario);
    	return loginDTO;
    }
    
    public void accesoNoPermitido(int idSegUsuario,String ruta, String direccionIP) {
    	LoginDTO loginDTO=new LoginDTO();
		loginDTO.setIdSegUsuario(idSegUsuario);
		loginDTO.setDireccionIP(direccionIP);
    	mAuditoria.mostrarLog(loginDTO, getClass(), "accesoNoPermitido", "Usuario "+idSegUsuario+" intento no autorizado a "+ruta);
    }
    
    public List<SegUsuario> findAllUsuarios(){
    	return mDAO.findAll(SegUsuario.class, "apellidos");
    }
    
    public SegUsuario findByIdSegUsuario(int idSegUsuario) throws Exception {
    	return (SegUsuario) mDAO.findById(SegUsuario.class, idSegUsuario);
    }
    
    public void insertarUsuario(SegUsuario nuevoUsuario) throws Exception {
    	nuevoUsuario.setCodigo("n/a");
    	mDAO.insertar(nuevoUsuario);
    }
    
    public void actualizarUsuario(LoginDTO loginDTO, SegUsuario edicionUsuario) throws Exception {
    	SegUsuario usuario=(SegUsuario) mDAO.findById(SegUsuario.class, edicionUsuario.getIdSegUsuario());
    	usuario.setApellidos(edicionUsuario.getApellidos());
    	usuario.setClave(edicionUsuario.getClave());
    	usuario.setCorreo(edicionUsuario.getCorreo());
    	usuario.setCodigo(edicionUsuario.getCodigo());
    	usuario.setNombres(edicionUsuario.getNombres());
    	mDAO.actualizar(usuario);
    	mAuditoria.mostrarLog(loginDTO,getClass(), "actualizarUsuario", "Usuario actualizado por: "+loginDTO.getIdSegUsuario());
    }
    
    public void activarDesactivarUsuario(int idSegUsuario) throws Exception {
    	SegUsuario usuario=(SegUsuario) mDAO.findById(SegUsuario.class, idSegUsuario);
    	if(idSegUsuario==1)
    		throw new Exception("No se puede desactivar al usuario administrador.");
    	mDAO.actualizar(usuario);
    }
    
    public void eliminarUsuario(int idSegUsuario, LoginDTO loginDTO) throws Exception {
    	SegUsuario usuario=(SegUsuario) mDAO.findById(SegUsuario.class, idSegUsuario);
    	if(usuario.getIdSegUsuario()==1)
    		throw new Exception("No se puede eliminar el usuario administrador.");
    	if(usuario.getSegAsignacions().size()>0)
    		throw new Exception("No se puede elimininar el usuario porque tiene asignaciones de modulos.");
    	mDAO.eliminar(SegUsuario.class, usuario.getIdSegUsuario());
    	mAuditoria.mostrarLog(loginDTO,getClass(), "eliminarUsuario", "Usuario eliminado por: "+loginDTO.getIdSegUsuario());
    }
    
    public List<SegModulo> findAllModulos(){
    	return mDAO.findAll(SegModulo.class, "nombreModulo");
    }
    
    public SegModulo insertarModulo(SegModulo nuevoModulo) throws Exception {
    	SegModulo modulo=new SegModulo();
    	modulo.setNombreModulo(nuevoModulo.getNombreModulo());
    	modulo.setRutaAcceso(nuevoModulo.getRutaAcceso());
    	mDAO.insertar(modulo);
    	return modulo;
    }
    
    public void eliminarModulo(int idSegModulo) throws Exception {
    	mDAO.eliminar(SegModulo.class, idSegModulo);
    }
    
    public void actualizarModulo(SegModulo edicionModulo) throws Exception {
    	SegModulo modulo=(SegModulo) mDAO.findById(SegModulo.class, edicionModulo.getIdSegModulo());
    	modulo.setNombreModulo(edicionModulo.getNombreModulo());
    	modulo.setRutaAcceso(edicionModulo.getRutaAcceso());
    	mDAO.actualizar(modulo);
    }
    
    public List<SegAsignacion> findAsignacionByUsuario(int idSegUsuario){
    	String consulta="o.segUsuario.idSegUsuario="+idSegUsuario;
		List<SegAsignacion> listaAsignaciones=mDAO.findWhere(SegAsignacion.class, consulta, null);
		return listaAsignaciones;
    }
    
    /**
     * Permite asignar el acceso a un modulo del inventario de sistemas.
     * @param idSegUsuario El codigo del usuario.
     * @param idSegModulo El codigo del modulo que se va a asignar.
     * @throws Exception
     */
    public void asignarModulo(int idSegUsuario,int idSegModulo) throws Exception{
    	//Buscar los objetos primarios:
    	SegUsuario usuario=(SegUsuario)mDAO.findById(SegUsuario.class, idSegUsuario);
    	SegModulo modulo=(SegModulo)mDAO.findById(SegModulo.class, idSegModulo);
    	//crear la relacion:
    	SegAsignacion asignacion=new SegAsignacion();
    	asignacion.setSegModulo(modulo);
    	asignacion.setSegUsuario(usuario);
    	//guardar la asignacion:
    	mDAO.insertar(asignacion);
    }
    
    public void eliminarAsignacion(int idSegAsignacion) throws Exception {
    	mDAO.eliminar(SegAsignacion.class, idSegAsignacion);
    }
    //conteo modulos
    public int ContarModulos() {
    	int numeroModulos=0;
    	List<SegModulo> mo=mDAO.findAll(SegModulo.class);
    	for(SegModulo m:mo) {
    		numeroModulos=numeroModulos+1;
		}
		return numeroModulos;
    }
  //conteo asignacion
    public int ContarAsignacion() {
    	int numeroAsignacion=0;
    	List<SegAsignacion> asig=mDAO.findAll(SegAsignacion.class);
    	for(SegAsignacion a:asig) {
    		numeroAsignacion=numeroAsignacion+1;
		}
		return numeroAsignacion;
    }
    //conteo usuario
    public int ContarUsuario() {
    	int numeroUsuario=0;
    	List<SegUsuario> usua=mDAO.findAll(SegUsuario.class);
    	for(SegUsuario usu:usua) {
    		numeroUsuario=numeroUsuario+1;
		}
		return numeroUsuario;
    }

}
