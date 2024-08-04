using Microsoft.AspNetCore.Mvc;
using Newtonsoft.Json;
using System.ComponentModel.DataAnnotations;
using Congratulator.Attributes;

using Congratulator.Models;

namespace Congratulator.Controllers
{ 
    [ApiController]
    public class BirthdaysController : ControllerBase
    {
        [HttpPost]
        [Route("/birthdays/new")]
        [ValidateModelState]
        public virtual IActionResult CreateNewBirthday([FromBody]Birthday body)
        { 
            string exampleJson = null;
            exampleJson = "{\n  \"message\" : \"Похоже на сервере произошёл сбой. Ваша проблема уже решается.\"\n}";
            
                        var example = exampleJson != null
                        ? JsonConvert.DeserializeObject<InlineResponseDefault>(exampleJson)
                        : default(InlineResponseDefault);
            return new ObjectResult(example);
        } 

        [HttpGet]
        [Route("/birthdays")]
        [ValidateModelState]
        public virtual IActionResult ReturnAllBirthdays()
        { 
            string exampleJson = null;
            exampleJson = "[ {\n  \"lastName\" : \"lastName\",\n  \"firstName\" : \"firstName\",\n  \"birthdayDate\" : \"2000-01-23T04:56:07.000+00:00\",\n  \"middleName\" : \"middleName\",\n  \"uuid\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"\n}, {\n  \"lastName\" : \"lastName\",\n  \"firstName\" : \"firstName\",\n  \"birthdayDate\" : \"2000-01-23T04:56:07.000+00:00\",\n  \"middleName\" : \"middleName\",\n  \"uuid\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"\n} ]";
            
                        var example = exampleJson != null
                        ? JsonConvert.DeserializeObject<AllBirthdays>(exampleJson)
                        : default(AllBirthdays);
            return new ObjectResult(example);
        }

        [HttpGet]
        [Route("/birthdays/upcoming")]
        [ValidateModelState]
        public virtual IActionResult ReturnActuallBirthdays([FromQuery][Required()][Range(1, 366)]int? period)
        { 
            throw new NotImplementedException();
        }

        [HttpPatch]
        [Route("/birthdays/сorrecting")]
        [ValidateModelState]
        public virtual IActionResult CorrectNewBirthday([FromBody]Birthday body)
        { 
            string exampleJson = null;
            exampleJson = "{\n  \"message\" : \"Похоже на сервере произошёл сбой. Ваша проблема уже решается.\"\n}";
            
                        var example = exampleJson != null
                        ? JsonConvert.DeserializeObject<InlineResponseDefault>(exampleJson)
                        : default(InlineResponseDefault);
            return new ObjectResult(example);
        }

       
        [HttpDelete]
        [Route("/birthdays/delete")]
        [ValidateModelState]
        public virtual IActionResult CorrectNewBirthday_0([FromBody]Birthday body)
        { 
            string exampleJson = null;
            exampleJson = "{\n  \"message\" : \"Похоже на сервере произошёл сбой. Ваша проблема уже решается.\"\n}";
            
                        var example = exampleJson != null
                        ? JsonConvert.DeserializeObject<InlineResponseDefault>(exampleJson)
                        : default(InlineResponseDefault);
            return new ObjectResult(example);
        }
    }
}
