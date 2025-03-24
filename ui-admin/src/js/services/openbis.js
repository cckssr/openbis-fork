import dto from '@src/js/services/openbis/dto.js';
import api from '@src/js/services/openbis/api.js';

class Openbis {
  constructor() {
    this.initialized = false;    
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
