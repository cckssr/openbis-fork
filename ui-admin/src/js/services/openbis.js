import dto from '@src/js/services/openbis/dto.js';
import api from '@src/js/services/openbis/api.js';

class Openbis {
  constructor() {
    this.initialized = false;
    // Provide sane defaults immediately; values will be overwritten after init with server values.
    this.DEFAULT_PACKAGE_SIZE_IN_BYTES = 10 * 1024 * 1024
    this.DEFAULT_TIMEOUT_IN_MILLIS = 30000
  }

  async init(v3) {    
    if (!this.initialized) {      
      try {        
        await Promise.all([
          dto._init(),
          api._init(v3)
        ]);
        Object.assign(this, dto);
        Object.assign(this, api);
        this.initialized = true;        
      } catch (error) {
        console.error("Error during Openbis initialization:", error);
        throw error;
      }
    }
  }
}

const openbis = new Openbis();
export default openbis;
