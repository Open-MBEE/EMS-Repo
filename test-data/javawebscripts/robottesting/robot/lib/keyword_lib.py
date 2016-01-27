from robot.api.deco import keyword

class Dynamic_Key_Lib:
    
    def get_keyword_names(self):
        return [name for name in dir(self) if hasattr(getattr(self, name), 'robot_name')]
    
    def helper_method(self):
        # Does something
        return false

    def execute_test(self,arguments):
        regression_lib.run_curl_test(test_num, test_name, test_desc, curl_cmd, 
                                    use_json_diff, filters, setupFcn, 
                                    postProcessFcn, teardownFcn, delay)