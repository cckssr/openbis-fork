var TestProtocol = new function () {

    this.getTestCount = function() {
        return 34; // If one test is broken, then all tests must be failed.
    }

    this.startAdminTests = function(withLogin) {
        testChain = Promise.resolve();

        if (withLogin) {
            //1. Login
            testChain.then(() => AdminTests.login())
        }
                 //2. Inventory Space and Sample Types
        testChain.then(() => AdminTests.inventorySpace())
                 //3. Settings Form - Enable Sample Types to Show in Drop-downs
                 .then(() => AdminTests.enableBacteriaToShowInDropDowns())
                 //4. Microscopy and Flow Cytometry plugin
                 .then(() => TestUtil.testLocally(4))
                 //5. User Manager
                 .then(() => AdminTests.userManager())
                 .catch(error => { console.log(error) });
    }

    this.startUserTests = function() {
        testChain = Promise.resolve();
                 //5. User Manager (end of test)
        testChain.then(() => UserTests.userManager(5))
                 //6. obsolete test
                 .then(() => TestUtil.testNotExist(6))
                 //7. obsolete test
                 .then(() => TestUtil.testNotExist(7))
                 //8. obsolete test
                 .then(() => TestUtil.testNotExist(8))
                 //9. obsolete test
                 .then(() => TestUtil.testNotExist(9))
                 //10. obsolete test
                 .then(() => TestUtil.testNotExist(10))
                 //11. obsolete test
                 .then(() => TestUtil.testNotExist(11))
                 //12. obsolete test
                 .then(() => TestUtil.testNotExist(12))
                 //13. obsolete test
                 .then(() => TestUtil.testNotExist(13))
                 //14. obsolete test
                 .then(() => TestUtil.testNotExist(14))
                 //15. obsolete test
                 .then(() => TestUtil.testNotExist(15))
                 //16. obsolete test
                 .then(() => TestUtil.testNotExist(16))
                 //17. obsolete test
                 .then(() => TestUtil.testNotExist(17))
                 //18. Create Protocol
                 .then(() => UserTests.createProtocol())
                 //19. Project Form - Create/Update
                 .then(() => UserTests.createProject())
                 //20. Experiment Form - Create/Update
                 .then(() => UserTests.createExperiment())
                 //21. Experiment Step Form - Create/Update
                 .then(() => UserTests.createExperimentStep())
                 //22. is now disabled
                 .then(() => TestUtil.testNotExist(22))
                 //23. Experiment Step Form - Dataset Uploader and Viewer
                 .then(() => UserTests.datasetUploader())
                 //24. Experiment Step Form - Children Generator (not exist)
                 .then(() => TestUtil.testNotExist(24))
                 //25. Project  Form - Show in project overview
                 .then(() => UserTests.showInProjectOverview())
                 //26. obsolete test
                 .then(() => TestUtil.testNotExist(26))
                 //27. Supplier Form
                 .then(() => UserTests.supplierForm())
                 //28. Product Form
                 .then(() => UserTests.productForm())
                 //29. Request Form
                 .then(() => UserTests.requestForm())
                 //30. Order Form
                 .then(() => UserTests.orderForm())
                 //31. logout
                 .then(() => UserTests.logout())
                 .catch(error => { console.log(error) });
    }

    this.finishTests = function() {
        testChain = Promise.resolve();
                 //31 . Order Form
        testChain.then(() => TestUtil.deleteCookies("suitename"))
                 .then(() => TestUtil.login("admin", "a"))
                 .then(() => AdminTests.orderForm())
                 //32. Order Form - Avoiding modifying orders by deleted requests
                 .then(() => AdminTests.deletedRequests())
                 //33. Trash Manager
                 .then(() => AdminTests.trashManager())
                 //34. Vocabulary Viewer
                 .then(() => AdminTests.vocabularyViewer())
                 //Tests passed
                 .then(() => TestUtil.allTestsPassed())
                 .catch(error => { console.log(error) });
    }
}
